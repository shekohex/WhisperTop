package me.shadykhalifa.whispertop.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.managers.OverlayInitializationManager
import me.shadykhalifa.whispertop.ui.notifications.OverlayNotificationManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Handles broadcast intents for overlay visibility control from notifications
 */
class OverlayVisibilityBroadcastReceiver : BroadcastReceiver(), KoinComponent {
    
    companion object {
        private const val TAG = "OverlayVisibilityReceiver"
    }
    
    private val overlayInitManager: OverlayInitializationManager by inject()
    private val overlayNotificationManager: OverlayNotificationManager by inject()
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) {
            Log.w(TAG, "Received null context or intent")
            return
        }
        
        val action = intent.action
        Log.d(TAG, "Received action: $action")
        
        when (action) {
            OverlayNotificationManager.ACTION_SHOW_OVERLAY -> {
                handleShowOverlay(context)
            }
            OverlayNotificationManager.ACTION_DISMISS_NOTIFICATION -> {
                handleDismissNotification()
            }
            else -> {
                Log.w(TAG, "Unknown action received: $action")
            }
        }
    }
    
    private fun handleShowOverlay(context: Context) {
        Log.d(TAG, "Handling show overlay action")
        try {
            // Dismiss the notification first
            overlayNotificationManager.dismissNotification()
            
            // Show the overlay in a coroutine since it may be async
            CoroutineScope(Dispatchers.Main).launch {
                overlayInitManager.showOverlayFromHidden()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error showing overlay", e)
        }
    }
    
    private fun handleDismissNotification() {
        Log.d(TAG, "Handling dismiss notification action")
        try {
            overlayNotificationManager.dismissNotification()
        } catch (e: Exception) {
            Log.e(TAG, "Error dismissing notification", e)
        }
    }
}