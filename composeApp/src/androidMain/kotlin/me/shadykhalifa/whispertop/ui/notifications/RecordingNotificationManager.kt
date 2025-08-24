package me.shadykhalifa.whispertop.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.service.AudioRecordingService
import java.util.Locale

/**
 * Enhanced notification manager for recording progress and feedback
 */
class RecordingNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "recording_progress"
        const val NOTIFICATION_ID = 1001
        private const val CHANNEL_NAME = "Recording Progress"
        private const val CHANNEL_DESCRIPTION = "Shows recording and transcription progress"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Update notification based on recording state
     * 
     * @param state The current recording state
     * @param audioLevel Current audio level (0.0 to 1.0)
     */
    fun updateNotification(state: RecordingState, audioLevel: Float = 0f) {
        try {
            val notification = when (state) {
                is RecordingState.Idle -> createIdleNotification()
                is RecordingState.Recording -> createRecordingNotification(state, audioLevel)
                is RecordingState.Processing -> createProcessingNotification(state)
                is RecordingState.Success -> createSuccessNotification(state)
                is RecordingState.Error -> createErrorNotification(state)
            }
            
            notificationManager.notify(NOTIFICATION_ID, notification.build())
        } catch (e: SecurityException) {
            android.util.Log.e("RecordingNotification", "Notification permission denied", e)
        } catch (e: Exception) {
            android.util.Log.e("RecordingNotification", "Failed to update notification", e)
        }
    }
    
    /**
     * Show recording duration notification
     * 
     * @param durationMs Recording duration in milliseconds
     * @param audioLevel Current audio level (0.0 to 1.0)
     */
    fun updateRecordingDuration(durationMs: Long, audioLevel: Float = 0f) {
        try {
            val minutes = (durationMs / 1000) / 60
            val seconds = (durationMs / 1000) % 60
            
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle(context.getString(R.string.notification_recording_title))
                .setContentText(context.getString(R.string.notification_recording_text, minutes, seconds))
                .setProgress(0, 0, true) // Indeterminate progress
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(createStopAction())
                .addAction(createPauseAction())
                .apply {
                    // Add audio level indicator if available
                    if (audioLevel > 0f) {
                        val levelPercent = (audioLevel * 100).toInt()
                        setSubText("Audio level: ${levelPercent}%")
                    }
                }
            
            notificationManager.notify(NOTIFICATION_ID, notification.build())
        } catch (e: SecurityException) {
            android.util.Log.e("RecordingNotification", "Notification permission denied", e)
        } catch (e: Exception) {
            android.util.Log.e("RecordingNotification", "Failed to update recording duration notification", e)
        }
    }
    
    /**
     * Show transcription progress
     */
    fun updateTranscriptionProgress(progress: Float) {
        val progressPercent = (progress * 100).toInt()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_processing)
            .setContentTitle("Transcribing Audio")
            .setContentText("Converting speech to text...")
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSubText("${progressPercent}% complete")
        
        notificationManager.notify(NOTIFICATION_ID, notification.build())
    }
    
    /**
     * Dismiss all notifications
     */
    fun dismissNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            android.util.Log.e("RecordingNotification", "Failed to dismiss notification", e)
        }
    }
    
    /**
     * Check if notification permissions are available
     * 
     * @return true if notification permission is granted
     */
    fun isNotificationPermissionGranted(): Boolean {
        return try {
            notificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            android.util.Log.e("RecordingNotification", "Failed to check notification permission", e)
            false
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = CHANNEL_DESCRIPTION
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createIdleNotification(): NotificationCompat.Builder {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle(context.getString(R.string.notification_idle_title))
            .setContentText(context.getString(R.string.notification_idle_text))
            .setAutoCancel(true)
            .setOngoing(false)
    }
    
    private fun createRecordingNotification(state: RecordingState.Recording, audioLevel: Float): NotificationCompat.Builder {
        val duration = formatDuration(state.duration)
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic_recording)
            .setContentTitle(context.getString(R.string.notification_recording_title))
            .setContentText("Recording: $duration")
            .setProgress(0, 0, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(createStopAction())
            .addAction(createPauseAction())
            .apply {
                if (audioLevel > 0f) {
                    val levelPercent = (audioLevel * 100).toInt()
                    setSubText("Audio level: ${levelPercent}%")
                }
            }
    }
    
    private fun createProcessingNotification(state: RecordingState.Processing): NotificationCompat.Builder {
        val progressPercent = (state.progress * 100).toInt()
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_processing)
            .setContentTitle(context.getString(R.string.notification_processing_title))
            .setContentText(context.getString(R.string.notification_processing_text))
            .setProgress(100, progressPercent, false)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSubText("${progressPercent}% complete")
    }
    
    private fun createSuccessNotification(state: RecordingState.Success): NotificationCompat.Builder {
        val previewText = state.transcription.take(50) + if (state.transcription.length > 50) "..." else ""
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_check)
            .setContentTitle("Transcription Complete")
            .setContentText(previewText)
            .setAutoCancel(true)
            .setOngoing(false)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(state.transcription)
                    .setSummaryText("Tap to dismiss")
            )
    }
    
    private fun createErrorNotification(state: RecordingState.Error): NotificationCompat.Builder {
        val errorMessage = state.throwable.message ?: "Unknown error"
        
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_error)
            .setContentTitle("Recording Error")
            .setContentText(errorMessage)
            .setAutoCancel(true)
            .setOngoing(false)
            .apply {
                if (state.retryable) {
                    addAction(createRetryAction())
                }
            }
    }
    
    private fun createStopAction(): NotificationCompat.Action {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = "action_stop_recording"
        }
        val pendingIntent = PendingIntent.getService(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_stop,
            context.getString(R.string.stop_recording),
            pendingIntent
        ).build()
    }
    
    private fun createPauseAction(): NotificationCompat.Action {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = "action_pause_recording"
        }
        val pendingIntent = PendingIntent.getService(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_pause,
            context.getString(R.string.pause_recording),
            pendingIntent
        ).build()
    }
    
    private fun createRetryAction(): NotificationCompat.Action {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = "action_start_recording"
        }
        val pendingIntent = PendingIntent.getService(
            context,
            2,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_retry,
            context.getString(R.string.try_again),
            pendingIntent
        ).build()
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}