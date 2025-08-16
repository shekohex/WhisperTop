package me.shadykhalifa.whispertop.managers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * BroadcastReceiver for handling system events that may require service restart
 */
class SystemEventReceiver : BroadcastReceiver(), KoinComponent {
    
    private val serviceRecoveryManager: ServiceRecoveryManager by inject()
    private val audioServiceManager: AudioServiceManager by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                handleBootCompleted(context)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_PACKAGE_REPLACED -> {
                if (intent.dataString?.contains(context.packageName) == true) {
                    handleAppUpdated(context)
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                handlePowerConnected(context)
            }
            Intent.ACTION_USER_PRESENT -> {
                handleUserPresent(context)
            }
            "android.intent.action.ACTION_SHUTDOWN" -> {
                handleSystemShutdown(context)
            }
        }
    }
    
    private fun handleBootCompleted(context: Context) {
        // Start services after device boot
        scope.launch {
            try {
                audioServiceManager.bindService()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    private fun handleAppUpdated(context: Context) {
        // Restart services after app update
        try {
            serviceRecoveryManager.forceServiceRestart()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun handlePowerConnected(context: Context) {
        // Check service health when power is connected
        try {
            serviceRecoveryManager.schedulePeriodicRecoveryCheck()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun handleUserPresent(context: Context) {
        // User unlocked device - good time to check service health
        try {
            if (!isServiceHealthy()) {
                serviceRecoveryManager.handleServiceCrash()
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    private fun handleSystemShutdown(context: Context) {
        // Clean shutdown
        try {
            audioServiceManager.unbindService()
        } catch (e: Exception) {
            // Handle error silently
        }
    }
    
    private fun isServiceHealthy(): Boolean {
        return audioServiceManager.connectionState.value == AudioServiceManager.ServiceConnectionState.CONNECTED
    }
    
    companion object {
        /**
         * Create intent filter for system events
         */
        fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction(Intent.ACTION_BOOT_COMPLETED)
                addAction(Intent.ACTION_MY_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_USER_PRESENT)
                addAction("android.intent.action.ACTION_SHUTDOWN")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    addDataScheme("package")
                }
            }
        }
    }
}