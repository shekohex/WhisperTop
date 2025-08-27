package me.shadykhalifa.whispertop.ui.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.receivers.OverlayVisibilityBroadcastReceiver

/**
 * Manages notifications for overlay visibility control
 */
class OverlayNotificationManager(private val context: Context) {
    
    companion object {
        const val CHANNEL_ID = "overlay_control"
        const val NOTIFICATION_ID = 2001
        private const val CHANNEL_NAME = "Overlay Control"
        private const val CHANNEL_DESCRIPTION = "Controls for showing or hiding the mic overlay"
        
        const val ACTION_SHOW_OVERLAY = "action_show_overlay"
        const val ACTION_DISMISS_NOTIFICATION = "action_dismiss_notification"
    }
    
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Show notification indicating the overlay is hidden with actions to show it again
     */
    fun showOverlayHiddenNotification() {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_mic)
                .setContentTitle(context.getString(R.string.notification_overlay_hidden_title))
                .setContentText(context.getString(R.string.notification_overlay_hidden_text))
                .setAutoCancel(false)
                .setOngoing(false)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .addAction(createShowOverlayAction())
                .addAction(createIgnoreAction())
                .build()
            
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            android.util.Log.e("OverlayNotification", "Notification permission denied", e)
        } catch (e: Exception) {
            android.util.Log.e("OverlayNotification", "Failed to show overlay hidden notification", e)
        }
    }
    
    /**
     * Dismiss the overlay control notification
     */
    fun dismissNotification() {
        try {
            notificationManager.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            android.util.Log.e("OverlayNotification", "Failed to dismiss overlay notification", e)
        }
    }
    
    /**
     * Check if notification permissions are available
     */
    fun isNotificationPermissionGranted(): Boolean {
        return try {
            notificationManager.areNotificationsEnabled()
        } catch (e: Exception) {
            android.util.Log.e("OverlayNotification", "Failed to check notification permission", e)
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
    
    private fun createShowOverlayAction(): NotificationCompat.Action {
        val intent = Intent(context, OverlayVisibilityBroadcastReceiver::class.java).apply {
            action = ACTION_SHOW_OVERLAY
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_mic,
            context.getString(R.string.action_show_overlay),
            pendingIntent
        ).build()
    }
    
    private fun createIgnoreAction(): NotificationCompat.Action {
        val intent = Intent(context, OverlayVisibilityBroadcastReceiver::class.java).apply {
            action = ACTION_DISMISS_NOTIFICATION
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Action.Builder(
            R.drawable.ic_delete,
            context.getString(R.string.action_ignore),
            pendingIntent
        ).build()
    }
}