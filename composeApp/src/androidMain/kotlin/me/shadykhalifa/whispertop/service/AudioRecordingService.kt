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
        private const val ACTION_STOP_RECORDING = "action_stop_recording"
        private const val ACTION_PAUSE_RECORDING = "action_pause_recording"
        private const val ACTION_RESUME_RECORDING = "action_resume_recording"
    }
    
    enum class RecordingState {
        IDLE, RECORDING, PAUSED, PROCESSING
    }
    
    private val audioRecorder: AudioRecorderImpl by inject()
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
        acquireWakeLock()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
        }
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        releaseWakeLock()
        if (currentState.get() == RecordingState.RECORDING) {
            scope.launch { stopRecording() }
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
                
                startForeground(NOTIFICATION_ID, createNotification())
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
                
                stopForeground(STOP_FOREGROUND_REMOVE)
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
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_description)
                setSound(null, null)
                enableVibration(false)
            }
            
            notificationManager?.createNotificationChannel(channel)
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
        
        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getNotificationTitle())
            .setContentText(getNotificationText())
            .setSmallIcon(R.drawable.ic_mic)
            .setContentIntent(mainPendingIntent)
            .addAction(R.drawable.ic_mic, getString(R.string.stop_recording), stopPendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
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
    
    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WhisperTop::AudioRecordingWakeLock"
        )
        wakeLock?.acquire(10 * 60 * 1000L) // 10 minutes
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