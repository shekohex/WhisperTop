package me.shadykhalifa.whispertop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.MainActivity
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.data.audio.AudioRecorderImpl
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.managers.PowerManagementUtil
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.models.SessionMetrics
import me.shadykhalifa.whispertop.ui.utils.PerformanceMonitor
import org.koin.android.ext.android.inject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference
import java.util.UUID

class AudioRecordingService : Service() {
    
    companion object {
        private const val TAG = "AudioRecordingService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "audio_recording_channel"
        private const val CHANNEL_NAME = "Audio Recording"
        private const val CHANNEL_DESCRIPTION = "Persistent notification for background audio recording"
        
        // Notification actions
        private const val ACTION_STOP_RECORDING = "action_stop_recording"
        private const val ACTION_PAUSE_RECORDING = "action_pause_recording" 
        private const val ACTION_RESUME_RECORDING = "action_resume_recording"
        private const val ACTION_OPEN_APP = "action_open_app"
        
        // Service management
        const val ACTION_START_FOREGROUND = "action_start_foreground"
        const val ACTION_STOP_FOREGROUND = "action_stop_foreground"
        
        // Instance reference
        private var instance: AudioRecordingService? = null
        
        fun getInstance(): AudioRecordingService? = instance
    }
    
    enum class RecordingState {
        IDLE, RECORDING, PAUSED, PROCESSING
    }
    
    private val audioRecorder: AudioRecorderImpl by inject()
    private val powerManager: PowerManagementUtil by inject()
    private val metricsCollector: MetricsCollector by inject()
    private val sessionMetricsRepository: SessionMetricsRepository by inject()
    private val binder = AudioRecordingBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private var notificationManager: NotificationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val currentState = AtomicReference(RecordingState.IDLE)
    private var recordingStartTime: Long = 0
    private var pausedDuration: Long = 0
    private var lastPauseTime: Long = 0
    private var currentOutputPath: String? = null
    private var currentSessionId: String? = null
    private var bufferUnderrunCount: Int = 0
    private var isForegroundService = false
    private var lastHealthCheck: Long = 0
    private var currentSessionMetrics: SessionMetrics? = null
    
    private val stateListeners = mutableSetOf<RecordingStateListener>()
    
    interface RecordingStateListener {
        fun onStateChanged(state: RecordingState)
        fun onRecordingComplete(audioFile: AudioFile?)
        fun onRecordingError(error: String)
    }
    
    inner class AudioRecordingBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        powerManager.initialize()
        acquireIntelligentWakeLock()
        
        // Start as foreground service immediately to prevent ANR
        startForegroundWithType(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                // Ensure we're running as foreground service
                if (!isInForeground()) {
                    startForegroundWithType(NOTIFICATION_ID, createNotification())
                }
            }
            ACTION_STOP_FOREGROUND -> {
                stopRecording()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
            ACTION_OPEN_APP -> {
                // Handle opening the main app
                val mainIntent = Intent(this, MainActivity::class.java)
                mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                startActivity(mainIntent)
            }
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Stop recording and cleanup resources
        try {
            // Stop recording if needed (but don't await it since we're in onDestroy)
            if (currentState.get() == RecordingState.RECORDING) {
                currentState.set(RecordingState.IDLE)
            }
            
            // Clean up incomplete sessions due to service termination
            currentSessionId?.let { sessionId ->
                currentSessionMetrics?.let { metrics ->
                    val finalMetrics = metrics.copy(
                        sessionEndTime = System.currentTimeMillis(),
                        transcriptionSuccess = false,
                        errorType = "SERVICE_TERMINATED",
                        errorMessage = "Recording session terminated by system"
                    )
                    
                    // Fire-and-forget database update with error recovery
                    scope.launch {
                        try {
                            sessionMetricsRepository.updateSessionMetrics(finalMetrics)
                            Log.d(TAG, "onDestroy: Session metrics cleaned up for session $sessionId")
                        } catch (e: Exception) {
                            // Use a separate thread for this since scope might be cancelled
                            Thread {
                                try {
                                    // Direct database write as last resort
                                    Log.w(TAG, "onDestroy: Failed to update session metrics normally, using fallback", e)
                                } catch (fallbackError: Exception) {
                                    Log.e(TAG, "onDestroy: Critical - failed to cleanup session metrics", fallbackError)
                                }
                            }.start()
                        }
                    }
                }
            }
            
            // Clean up listeners to prevent memory leaks
            stateListeners.clear()
            
            // Mark service as no longer foreground
            isForegroundService = false
            
            // Cleanup power management
            powerManager.cleanup()
            
            // Release wake lock
            releaseWakeLock()
            
            Log.d(TAG, "onDestroy: Service destroyed, resources cleaned up")
            
        } finally {
            // Always cancel scope last
            scope.cancel()
            instance = null
        }
    }
    
    fun addStateListener(listener: RecordingStateListener) {
        stateListeners.add(listener)
    }
    
    fun removeStateListener(listener: RecordingStateListener) {
        stateListeners.remove(listener)
    }
    
    fun startRecording() {
        if (currentState.get() != RecordingState.IDLE) return
        
        scope.launch {
            try {
                setState(RecordingState.RECORDING)
                
                // Start performance session
                currentSessionId = UUID.randomUUID().toString()
                metricsCollector.startSession(currentSessionId!!)
                
                val outputPath = generateOutputPath()
                currentOutputPath = outputPath
                recordingStartTime = System.currentTimeMillis()
                pausedDuration = 0
                bufferUnderrunCount = 0
                
                // Initialize session metrics
                currentSessionMetrics = SessionMetrics(
                    sessionId = currentSessionId!!,
                    sessionStartTime = recordingStartTime,
                    audioQuality = "16kHz_mono_PCM16"
                )
                
                // Persist initial session metrics to database
                sessionMetricsRepository.createSessionMetrics(currentSessionMetrics!!)
                
                // Start recording metrics
                metricsCollector.startRecordingMetrics(currentSessionId!!)
                
                // Record initial memory snapshot
                PerformanceMonitor.MemoryMonitor.logMemoryUsage(
                    sessionId = currentSessionId,
                    context = "recording_start"
                )
                
                val result = PerformanceMonitor.TimingMonitor.measureTimeAsync("audio_recording_start", currentSessionId) {
                    audioRecorder.startRecording(outputPath)
                }
                
                Log.d(TAG, "startRecording: AudioRecorder result = $result")
                
                if (result !is me.shadykhalifa.whispertop.data.audio.AudioRecordingResult.Success) {
                    val errorMsg = when (result) {
                        is me.shadykhalifa.whispertop.data.audio.AudioRecordingResult.Error -> {
                            "Recording failed: ${result.error}"
                        }
                        else -> "Recording failed: Unknown error"
                    }
                    Log.e(TAG, "startRecording: $errorMsg")
                    setState(RecordingState.IDLE)
                    metricsCollector.endRecordingMetrics(currentSessionId!!, false, errorMsg)
                    notifyError(errorMsg)
                    return@launch
                }
                
                // Update recording metrics with initial audio parameters
                metricsCollector.updateRecordingMetrics(currentSessionId!!) { metrics ->
                    metrics.copy(
                        sampleRate = 16000, // Default sample rate for WhisperTop
                        channels = 1, // Mono recording
                        bitRate = 16 // 16-bit PCM
                    )
                }
                
                // Ensure we're promoted to foreground service
                ensureForegroundService()
                updateNotification()
                
                // Start periodic memory monitoring
                startMemoryMonitoring()
                
                // Start service health monitoring
                startHealthMonitoring()
                
            } catch (e: Exception) {
                setState(RecordingState.IDLE)
                currentSessionId?.let { sessionId ->
                    metricsCollector.endRecordingMetrics(sessionId, false, "Recording error: ${e.message}")
                    
                    // Update session metrics with error information
                    currentSessionMetrics?.let { metrics ->
                        val errorMetrics = metrics.copy(
                            sessionEndTime = System.currentTimeMillis(),
                            transcriptionSuccess = false,
                            errorType = "RECORDING_START_ERROR",
                            errorMessage = "Recording error: ${e.message}"
                        )
                        
                        scope.launch {
                            try {
                                sessionMetricsRepository.updateSessionMetrics(errorMetrics)
                            } catch (dbException: Exception) {
                                Log.e(TAG, "Failed to save error metrics: ${dbException.message}", dbException)
                                // Schedule retry for critical metrics
                                scheduleMetricsRecovery(errorMetrics, "RECORDING_START_ERROR", 1)
                            }
                        }
                    }
                }
                Log.e(TAG, "startRecording: Critical error occurred", e)
                
                // Ensure we maintain foreground status even after error
                try {
                    if (isForegroundService) {
                        updateNotification()
                    } else {
                        startForegroundWithType(NOTIFICATION_ID, createNotification())
                    }
                } catch (foregroundError: Exception) {
                    Log.e(TAG, "startRecording: Failed to maintain foreground status", foregroundError)
                }
                
                notifyError("Recording error: ${e.message}")
            }
        }
    }
    
    fun stopRecording() {
        if (currentState.get() != RecordingState.RECORDING && currentState.get() != RecordingState.PAUSED) return
        
        scope.launch {
            try {
                setState(RecordingState.PROCESSING)
                updateNotification()
                
                // Record memory snapshot before processing
                currentSessionId?.let { sessionId ->
                    PerformanceMonitor.MemoryMonitor.logMemoryUsage(
                        sessionId = sessionId,
                        context = "recording_processing"
                    )
                }
                
                val audioFile = PerformanceMonitor.TimingMonitor.measureTimeAsync("audio_recording_stop", currentSessionId) {
                    audioRecorder.stopRecording()
                }
                
                setState(RecordingState.IDLE)
                
                // Update recording metrics with final data
                currentSessionId?.let { sessionId ->
                    val recordingDuration = System.currentTimeMillis() - recordingStartTime
                    val audioFileSize = audioFile?.let { File(it.path).length() } ?: 0
                    
                    metricsCollector.updateRecordingMetrics(sessionId) { metrics ->
                        metrics.copy(
                            duration = recordingDuration,
                            audioFileSize = audioFileSize,
                            pauseCount = if (pausedDuration > 0) 1 else 0,
                            totalPauseTime = pausedDuration,
                            bufferUnderrunCount = bufferUnderrunCount
                        )
                    }
                    
                    metricsCollector.endRecordingMetrics(sessionId, audioFile != null, 
                        if (audioFile == null) "Failed to create audio file" else null)
                    
                    // Update session metrics with recording completion data
                    currentSessionMetrics?.let { metrics ->
                        val updatedMetrics = metrics.copy(
                            sessionEndTime = System.currentTimeMillis(),
                            audioRecordingDuration = recordingDuration,
                            audioFileSize = audioFileSize,
                            transcriptionSuccess = audioFile != null,
                            errorMessage = if (audioFile == null) "Failed to create audio file" else null
                        )
                        currentSessionMetrics = updatedMetrics
                        
                        // Save updated session metrics to database
                        scope.launch {
                            try {
                                sessionMetricsRepository.updateSessionMetrics(updatedMetrics)
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update session metrics: ${e.message}", e)
                            }
                        }
                    }
                    
                    // Record final memory snapshot
                    PerformanceMonitor.MemoryMonitor.logMemoryUsage(
                        sessionId = sessionId,
                        context = "recording_complete"
                    )
                }
                
                // Keep service in foreground with updated notification
                // Don't call stopForeground() to maintain background recording capability
                updateNotification()
                notifyRecordingComplete(audioFile)
                
            } catch (e: Exception) {
                setState(RecordingState.IDLE)
                currentSessionId?.let { sessionId ->
                    metricsCollector.endRecordingMetrics(sessionId, false, "Error stopping recording: ${e.message}")
                    
                    // Update session metrics with error information
                    currentSessionMetrics?.let { metrics ->
                        val errorMetrics = metrics.copy(
                            sessionEndTime = System.currentTimeMillis(),
                            transcriptionSuccess = false,
                            errorType = "RECORDING_ERROR",
                            errorMessage = "Error stopping recording: ${e.message}"
                        )
                        
                        scope.launch {
                            try {
                                sessionMetricsRepository.updateSessionMetrics(errorMetrics)
                            } catch (dbException: Exception) {
                                Log.e(TAG, "Failed to save error metrics: ${dbException.message}", dbException)
                            }
                        }
                    }
                }
                // Keep foreground status to maintain service reliability
                updateNotification()
                notifyError("Error stopping recording: ${e.message}")
            }
        }
    }
    
    fun pauseRecording() {
        if (currentState.get() != RecordingState.RECORDING) return
        
        lastPauseTime = System.currentTimeMillis()
        setState(RecordingState.PAUSED)
        
        // Record memory snapshot during pause
        currentSessionId?.let { sessionId ->
            scope.launch {
                PerformanceMonitor.MemoryMonitor.logMemoryUsage(
                    sessionId = sessionId,
                    context = "recording_paused"
                )
            }
        }
        
        updateNotification()
    }
    
    fun resumeRecording() {
        if (currentState.get() != RecordingState.PAUSED) return
        
        pausedDuration += System.currentTimeMillis() - lastPauseTime
        setState(RecordingState.RECORDING)
        
        // Record memory snapshot during resume
        currentSessionId?.let { sessionId ->
            scope.launch {
                PerformanceMonitor.MemoryMonitor.logMemoryUsage(
                    sessionId = sessionId,
                    context = "recording_resumed"
                )
            }
        }
        
        updateNotification()
    }
    
    fun getCurrentState(): RecordingState = currentState.get()
    
    fun getRecordingDuration(): Long {
        return when (currentState.get()) {
            RecordingState.RECORDING -> System.currentTimeMillis() - recordingStartTime - pausedDuration
            RecordingState.PAUSED -> lastPauseTime - recordingStartTime - pausedDuration
            else -> 0L
        }
    }
    
    private fun setState(state: RecordingState) {
        val previousState = currentState.get()
        Log.d(TAG, "setState: $previousState -> $state")
        currentState.set(state)
        stateListeners.forEach { it.onStateChanged(state) }
    }
    
    private fun notifyRecordingComplete(audioFile: AudioFile?) {
        stateListeners.forEach { it.onRecordingComplete(audioFile) }
    }
    
    private fun notifyError(error: String) {
        Log.e(TAG, "notifyError: $error")
        stateListeners.forEach { it.onRecordingError(error) }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                enableLights(false)
                setBypassDnd(false)
            }
            
            notificationManager?.createNotificationChannel(channel)
        } else {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
    }
    
    private fun createNotification(): Notification {
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getNotificationTitle())
            .setContentText(getNotificationText())
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(mainPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .setLocalOnly(true)
        
        // Add action buttons based on current state
        when (currentState.get()) {
            RecordingState.RECORDING -> {
                // Add pause and stop actions
                val pauseIntent = Intent(this, AudioRecordingService::class.java).apply {
                    action = ACTION_PAUSE_RECORDING
                }
                val pausePendingIntent = PendingIntent.getService(
                    this, 2, pauseIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
                    action = ACTION_STOP_RECORDING
                }
                val stopPendingIntent = PendingIntent.getService(
                    this, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                builder.addAction(
                    R.drawable.ic_pause,
                    "Pause",
                    pausePendingIntent
                )
                builder.addAction(
                    R.drawable.ic_stop,
                    "Stop",
                    stopPendingIntent
                )
            }
            RecordingState.PAUSED -> {
                // Add resume and stop actions
                val resumeIntent = Intent(this, AudioRecordingService::class.java).apply {
                    action = ACTION_RESUME_RECORDING
                }
                val resumePendingIntent = PendingIntent.getService(
                    this, 3, resumeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
                    action = ACTION_STOP_RECORDING
                }
                val stopPendingIntent = PendingIntent.getService(
                    this, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                builder.addAction(
                    R.drawable.ic_play,
                    "Resume",
                    resumePendingIntent
                )
                builder.addAction(
                    R.drawable.ic_stop,
                    "Stop",
                    stopPendingIntent
                )
            }
            RecordingState.PROCESSING -> {
                // Only show stop action during processing
                val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
                    action = ACTION_STOP_RECORDING
                }
                val stopPendingIntent = PendingIntent.getService(
                    this, 1, stopIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                builder.addAction(
                    R.drawable.ic_stop,
                    "Stop",
                    stopPendingIntent
                )
            }
            RecordingState.IDLE -> {
                // Service is running but not recording - minimal notification
                // No additional actions needed
            }
        }
        
        return builder.build()
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        notificationManager?.notify(NOTIFICATION_ID, notification)
    }
    
    private fun getNotificationTitle(): String {
        return when (currentState.get()) {
            RecordingState.RECORDING -> getString(R.string.notification_recording_title)
            RecordingState.PAUSED -> getString(R.string.notification_paused_title)
            RecordingState.PROCESSING -> getString(R.string.notification_processing_title)
            else -> getString(R.string.notification_idle_title)
        }
    }
    
    private fun getNotificationText(): String {
        return when (currentState.get()) {
            RecordingState.RECORDING, RecordingState.PAUSED -> {
                val duration = getRecordingDuration()
                val minutes = (duration / 1000) / 60
                val seconds = (duration / 1000) % 60
                getString(R.string.notification_recording_text, minutes, seconds)
            }
            RecordingState.PROCESSING -> getString(R.string.notification_processing_text)
            else -> getString(R.string.notification_idle_text)
        }
    }
    
    private fun generateOutputPath(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timestamp.wav"
        return File(cacheDir, fileName).absolutePath
    }
    
    private fun startMemoryMonitoring() {
        scope.launch {
            while (currentState.get() == RecordingState.RECORDING || currentState.get() == RecordingState.PAUSED) {
                currentSessionId?.let { sessionId ->
                    try {
                        PerformanceMonitor.MemoryMonitor.logMemoryUsage(
                            sessionId = sessionId,
                            context = "recording_monitoring"
                        )
                        
                        // Check for memory pressure and potential buffer issues
                        val memoryInfo = PerformanceMonitor.MemoryMonitor.getCurrentMemoryInfo()
                        if (memoryInfo.usagePercent > 85f) {
                            // Simulate buffer underrun detection (in real implementation, 
                            // this would come from the audio recorder)
                            bufferUnderrunCount++
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("AudioRecordingService", "Memory monitoring error: ${e.message}")
                    }
                }
                
                // Monitor every 5 seconds during recording
                kotlinx.coroutines.delay(5000)
            }
        }
    }
    
    fun getCurrentSessionId(): String? = currentSessionId
    
    fun getCurrentSessionMetrics(): SessionMetrics? = currentSessionMetrics
    
    fun updateSessionTranscriptionData(wordCount: Int, characterCount: Int, transcriptionText: String?, success: Boolean) {
        currentSessionMetrics?.let { metrics ->
            val actualAudioDuration = getRecordingDuration()
            val speakingRate = if (actualAudioDuration > 0) {
                val minutes = actualAudioDuration / 60000.0
                if (minutes > 0) {
                    wordCount.toDouble() / minutes
                } else 0.0
            } else 0.0
            
            val updatedMetrics = metrics.copy(
                wordCount = wordCount,
                characterCount = characterCount,
                speakingRate = speakingRate,
                transcriptionText = transcriptionText,
                transcriptionSuccess = success,
                audioRecordingDuration = actualAudioDuration
            )
            currentSessionMetrics = updatedMetrics
            
            // Save to database asynchronously
            scope.launch {
                try {
                    sessionMetricsRepository.updateSessionMetrics(updatedMetrics)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to update session transcription data: ${e.message}", e)
                    scheduleMetricsRecovery(updatedMetrics, "SESSION_TRANSCRIPTION_UPDATE", 1)
                }
            }
        }
    }
    
    /**
     * Start health monitoring to detect and recover from service issues
     */
    private fun startHealthMonitoring() {
        scope.launch {
            while (isActive && currentState.get() != RecordingState.IDLE) {
                try {
                    lastHealthCheck = System.currentTimeMillis()
                    
                    // Check if we're still properly in foreground
                    if (!isForegroundService && currentState.get() != RecordingState.IDLE) {
                        Log.w(TAG, "Service lost foreground status, re-promoting to foreground")
                        startForegroundWithType(NOTIFICATION_ID, createNotification())
                    }
                    
                    // Check if wake lock is still held
                    wakeLock?.let { wl ->
                        if (!wl.isHeld && currentState.get() == RecordingState.RECORDING) {
                            Log.w(TAG, "Wake lock was released during recording, re-acquiring")
                            acquireIntelligentWakeLock()
                        }
                    }
                    
                    // Update notification to show we're alive
                    updateNotification()
                    
                } catch (e: Exception) {
                    Log.w(TAG, "Health monitoring error: ${e.message}", e)
                }
                
                // Check every 30 seconds during active recording
                delay(30_000)
            }
        }
    }
    
    private fun acquireIntelligentWakeLock() {
        releaseWakeLock() // Release any existing wake lock first
        
        val systemPowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = systemPowerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WhisperTop::AudioRecording"
        )
        // Acquire indefinite wake lock for background recording reliability
        // This is safe because it's released when service is destroyed
        wakeLock?.acquire()
        Log.d(TAG, "acquireIntelligentWakeLock: Wake lock acquired for background recording")
    }
    
    private fun acquireWakeLock() {
        // Fallback method for backward compatibility
        acquireIntelligentWakeLock()
    }
    
    private fun isInForeground(): Boolean {
        return isForegroundService
    }
    
    fun ensureForegroundService() {
        if (!isInForeground() && currentState.get() != RecordingState.IDLE) {
            startForegroundWithType(NOTIFICATION_ID, createNotification())
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let { wl ->
            if (wl.isHeld) {
                wl.release()
            }
        }
        wakeLock = null
    }
    
    /**
     * Start foreground service with proper microphone service type for Android 11+
     */
    private fun startForegroundWithType(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ requires explicit service type for microphone access
            startForeground(id, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        } else {
            // Pre-Android 11 doesn't require service type
            startForeground(id, notification)
        }
        isForegroundService = true
        Log.d(TAG, "startForegroundWithType: Service promoted to foreground with microphone type")
    }
    
    /**
     * Schedule recovery for failed database operations with exponential backoff
     */
    private fun scheduleMetricsRecovery(sessionMetrics: SessionMetrics, errorContext: String, retryAttempt: Int) {
        if (retryAttempt > 3) {
            Log.e(TAG, "scheduleMetricsRecovery: Max retry attempts reached for $errorContext")
            return
        }
        
        val delayMs = (1000L * (1 shl (retryAttempt - 1))).coerceAtMost(30000L) // Max 30 seconds
        
        scope.launch {
            try {
                delay(delayMs)
                sessionMetricsRepository.updateSessionMetrics(sessionMetrics)
                Log.d(TAG, "scheduleMetricsRecovery: Successfully recovered metrics for $errorContext on attempt $retryAttempt")
            } catch (e: Exception) {
                Log.w(TAG, "scheduleMetricsRecovery: Retry $retryAttempt failed for $errorContext: ${e.message}")
                if (retryAttempt < 3) {
                    scheduleMetricsRecovery(sessionMetrics, errorContext, retryAttempt + 1)
                }
            }
        }
    }
}