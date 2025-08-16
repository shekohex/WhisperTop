package me.shadykhalifa.whispertop.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.MainActivity
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.data.audio.AudioRecorderImpl
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.managers.PowerManagementUtil
import me.shadykhalifa.whispertop.managers.createWakeLockConfig
import org.koin.android.ext.android.inject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

class AudioRecordingService : Service() {
    
    companion object {
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
    }
    
    enum class RecordingState {
        IDLE, RECORDING, PAUSED, PROCESSING
    }
    
    private val audioRecorder: AudioRecorderImpl by inject()
    private val powerManager: PowerManagementUtil by inject()
    private val binder = AudioRecordingBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var notificationManager: NotificationManager? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val currentState = AtomicReference(RecordingState.IDLE)
    private var recordingStartTime: Long = 0
    private var pausedDuration: Long = 0
    private var lastPauseTime: Long = 0
    private var currentOutputPath: String? = null
    
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
        createNotificationChannel()
        powerManager.initialize()
        acquireIntelligentWakeLock()
        
        // Start as foreground service immediately to prevent ANR
        startForeground(NOTIFICATION_ID, createNotification())
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_FOREGROUND -> {
                // Ensure we're running as foreground service
                if (!isInForeground()) {
                    startForeground(NOTIFICATION_ID, createNotification())
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
            
            // Cleanup power management
            powerManager.cleanup()
            
            // Release wake lock
            releaseWakeLock()
            
        } finally {
            // Always cancel scope last
            scope.cancel()
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
                
                val outputPath = generateOutputPath()
                currentOutputPath = outputPath
                recordingStartTime = System.currentTimeMillis()
                pausedDuration = 0
                
                val result = audioRecorder.startRecording(outputPath)
                if (result !is me.shadykhalifa.whispertop.data.audio.AudioRecordingResult.Success) {
                    setState(RecordingState.IDLE)
                    notifyError("Failed to start recording")
                    return@launch
                }
                
                // Ensure we're promoted to foreground service
                ensureForegroundService()
                updateNotification()
                
            } catch (e: Exception) {
                setState(RecordingState.IDLE)
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
                
                val audioFile = audioRecorder.stopRecording()
                setState(RecordingState.IDLE)
                
                // Keep service running but remove foreground status
                stopForeground(STOP_FOREGROUND_DETACH)
                updateNotification()
                notifyRecordingComplete(audioFile)
                
            } catch (e: Exception) {
                setState(RecordingState.IDLE)
                stopForeground(STOP_FOREGROUND_REMOVE)
                notifyError("Error stopping recording: ${e.message}")
            }
        }
    }
    
    fun pauseRecording() {
        if (currentState.get() != RecordingState.RECORDING) return
        
        lastPauseTime = System.currentTimeMillis()
        setState(RecordingState.PAUSED)
        updateNotification()
    }
    
    fun resumeRecording() {
        if (currentState.get() != RecordingState.PAUSED) return
        
        pausedDuration += System.currentTimeMillis() - lastPauseTime
        setState(RecordingState.RECORDING)
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
        currentState.set(state)
        stateListeners.forEach { it.onStateChanged(state) }
    }
    
    private fun notifyRecordingComplete(audioFile: AudioFile?) {
        stateListeners.forEach { it.onRecordingComplete(audioFile) }
    }
    
    private fun notifyError(error: String) {
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
        val recordingsDir = File(filesDir, "recordings")
        if (!recordingsDir.exists()) {
            recordingsDir.mkdirs()
        }
        return File(recordingsDir, fileName).absolutePath
    }
    
    private fun acquireIntelligentWakeLock() {
        releaseWakeLock() // Release any existing wake lock first
        
        val config = powerManager.createWakeLockConfig(
            purpose = "AudioRecording",
            baseDuration = 10 * 60 * 1000L // 10 minutes base duration
        )
        
        wakeLock = powerManager.acquireIntelligentWakeLock(config)
    }
    
    private fun acquireWakeLock() {
        // Fallback method for backward compatibility
        acquireIntelligentWakeLock()
    }
    
    private fun isInForeground(): Boolean {
        return try {
            // Check if service is running in foreground
            // This is a simplified check - in practice, we track this state
            currentState.get() != RecordingState.IDLE
        } catch (e: Exception) {
            false
        }
    }
    
    fun ensureForegroundService() {
        if (!isInForeground() && currentState.get() != RecordingState.IDLE) {
            startForeground(NOTIFICATION_ID, createNotification())
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
}