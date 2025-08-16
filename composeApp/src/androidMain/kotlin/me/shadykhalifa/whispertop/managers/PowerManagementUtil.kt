package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.PowerManager
import android.content.BroadcastReceiver
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Utility class for managing power optimization features like Doze mode and App Standby
 */
class PowerManagementUtil(private val context: Context) {
    
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    private val _powerState = MutableStateFlow(PowerState())
    val powerState: StateFlow<PowerState> = _powerState.asStateFlow()
    
    private var powerReceiver: PowerBroadcastReceiver? = null
    
    data class PowerState(
        val isInDozeMode: Boolean = false,
        val isDeviceIdle: Boolean = false,
        val isInPowerSaveMode: Boolean = false,
        val isIgnoringBatteryOptimizations: Boolean = false,
        val networkRestricted: Boolean = false,
        val backgroundRestricted: Boolean = false
    )
    
    data class WakeLockConfig(
        val tag: String,
        val timeout: Long,
        val flags: Int = PowerManager.PARTIAL_WAKE_LOCK
    )
    
    /**
     * Initialize power management monitoring
     */
    fun initialize() {
        registerPowerReceiver()
        updatePowerState()
    }
    
    /**
     * Clean up resources
     */
    fun cleanup() {
        unregisterPowerReceiver()
    }
    
    /**
     * Check if device is currently in Doze mode
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun isInDozeMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                powerManager.isDeviceIdleMode
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Check if device is in power save mode
     */
    fun isInPowerSaveMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                powerManager.isPowerSaveMode
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Check if app is whitelisted from battery optimization
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } catch (e: Exception) {
                false
            }
        } else {
            true // Not applicable on older versions
        }
    }
    
    /**
     * Acquire wake lock with intelligent timeout based on power state
     */
    fun acquireIntelligentWakeLock(config: WakeLockConfig): PowerManager.WakeLock? {
        return try {
            val adjustedTimeout = calculateOptimalTimeout(config.timeout)
            val wakeLock = powerManager.newWakeLock(config.flags, config.tag)
            
            if (adjustedTimeout > 0) {
                wakeLock.acquire(adjustedTimeout)
            } else {
                wakeLock.acquire()
            }
            
            wakeLock
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Calculate optimal timeout based on current power conditions
     */
    private fun calculateOptimalTimeout(requestedTimeout: Long): Long {
        val currentState = _powerState.value
        
        return when {
            currentState.isInPowerSaveMode -> requestedTimeout / 2 // Reduce by 50% in power save mode
            currentState.isInDozeMode -> requestedTimeout / 4 // Reduce by 75% in doze mode
            !currentState.isIgnoringBatteryOptimizations -> requestedTimeout / 3 // Reduce by 66% if not whitelisted
            else -> requestedTimeout // Use full timeout if optimized
        }
    }
    
    /**
     * Check if network operations should be delayed
     */
    fun shouldDelayNetworkOperations(): Boolean {
        val currentState = _powerState.value
        return currentState.isInDozeMode || 
               (currentState.isInPowerSaveMode && !currentState.isIgnoringBatteryOptimizations)
    }
    
    /**
     * Check if background processing should be limited
     */
    fun shouldLimitBackgroundProcessing(): Boolean {
        val currentState = _powerState.value
        return currentState.isInDozeMode || currentState.backgroundRestricted
    }
    
    /**
     * Get recommended delay for non-critical operations during power saving
     */
    fun getRecommendedOperationDelay(): Long {
        val currentState = _powerState.value
        
        return when {
            currentState.isInDozeMode -> 30_000L // 30 seconds
            currentState.isInPowerSaveMode -> 15_000L // 15 seconds
            !currentState.isIgnoringBatteryOptimizations -> 10_000L // 10 seconds
            else -> 0L // No delay
        }
    }
    
    /**
     * Check if it's safe to perform intensive operations
     */
    fun isOptimalForIntensiveOperations(): Boolean {
        val currentState = _powerState.value
        return !currentState.isInDozeMode && 
               !currentState.isInPowerSaveMode && 
               currentState.isIgnoringBatteryOptimizations
    }
    
    /**
     * Register broadcast receiver for power state changes
     */
    private fun registerPowerReceiver() {
        powerReceiver = PowerBroadcastReceiver { updatePowerState() }
        
        val filter = IntentFilter().apply {
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
            }
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        
        try {
            context.registerReceiver(powerReceiver, filter)
        } catch (e: Exception) {
            // Handle registration failure
        }
    }
    
    /**
     * Unregister broadcast receiver
     */
    private fun unregisterPowerReceiver() {
        powerReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Handle unregistration failure
            }
        }
        powerReceiver = null
    }
    
    /**
     * Update current power state
     */
    private fun updatePowerState() {
        val newState = PowerState(
            isInDozeMode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) isInDozeMode() else false,
            isDeviceIdle = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) isInDozeMode() else false,
            isInPowerSaveMode = isInPowerSaveMode(),
            isIgnoringBatteryOptimizations = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                isIgnoringBatteryOptimizations()
            } else {
                true
            },
            networkRestricted = shouldRestrictNetwork(),
            backgroundRestricted = shouldRestrictBackground()
        )
        
        _powerState.value = newState
    }
    
    /**
     * Check if network should be restricted based on current conditions
     */
    private fun shouldRestrictNetwork(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isInDozeMode() || (isInPowerSaveMode() && !isIgnoringBatteryOptimizations())
        } else {
            false
        }
    }
    
    /**
     * Check if background processing should be restricted
     */
    private fun shouldRestrictBackground(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            isInDozeMode()
        } else {
            false
        }
    }
    
    /**
     * Broadcast receiver for power management events
     */
    private class PowerBroadcastReceiver(
        private val onPowerStateChanged: () -> Unit
    ) : BroadcastReceiver() {
        
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                PowerManager.ACTION_POWER_SAVE_MODE_CHANGED,
                PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED,
                Intent.ACTION_SCREEN_ON,
                Intent.ACTION_SCREEN_OFF -> {
                    onPowerStateChanged()
                }
            }
        }
    }
}

/**
 * Extension function to create smart wake lock configurations
 */
fun PowerManagementUtil.createWakeLockConfig(
    purpose: String,
    baseDuration: Long,
    flags: Int = PowerManager.PARTIAL_WAKE_LOCK
): PowerManagementUtil.WakeLockConfig {
    return PowerManagementUtil.WakeLockConfig(
        tag = "WhisperTop::$purpose",
        timeout = baseDuration,
        flags = flags
    )
}