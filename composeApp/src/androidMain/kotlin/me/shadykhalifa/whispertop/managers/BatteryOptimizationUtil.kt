package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Utility class for detecting and managing battery optimization settings
 */
class BatteryOptimizationUtil(private val context: Context) {
    
    private val powerManager: PowerManager by lazy {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    
    /**
     * Data class representing the current battery optimization status
     */
    data class BatteryOptimizationStatus(
        val isIgnoringBatteryOptimizations: Boolean,
        val canRequestIgnore: Boolean,
        val isFeatureAvailable: Boolean,
        val explanation: String
    )
    
    /**
     * Checks if the app is currently whitelisted from battery optimization
     * @return true if the app is exempt from battery optimization, false otherwise
     */
    fun isIgnoringBatteryOptimizations(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } catch (e: Exception) {
                // Some devices may not support this feature or throw exceptions
                false
            }
        } else {
            // Battery optimization not available on older versions
            true
        }
    }
    
    /**
     * Checks if the app can request battery optimization exemption
     * @return true if the app can make the request, false otherwise
     */
    fun canRequestBatteryOptimizationExemption(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Check if the device supports this feature and if we have the permission
                val hasPermission = context.checkSelfPermission(
                    android.Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                hasPermission && isFeatureAvailable()
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Checks if battery optimization feature is available on this device
     * @return true if the feature is available, false otherwise
     */
    fun isFeatureAvailable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Try to access the PowerManager to see if it's available
                powerManager.isDeviceIdleMode // This will throw if not available
                true
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
    }
    
    /**
     * Gets comprehensive battery optimization status information
     * @return BatteryOptimizationStatus with detailed information
     */
    fun getBatteryOptimizationStatus(): BatteryOptimizationStatus {
        val isIgnoring = isIgnoringBatteryOptimizations()
        val canRequest = canRequestBatteryOptimizationExemption()
        val isAvailable = isFeatureAvailable()
        
        val explanation = when {
            !isAvailable -> "Battery optimization features not available on this device"
            isIgnoring -> "App is exempted from battery optimization - background recording should work reliably"
            canRequest -> "App is subject to battery optimization - background recording may be interrupted"
            else -> "Battery optimization exemption cannot be requested - manual settings adjustment may be required"
        }
        
        return BatteryOptimizationStatus(
            isIgnoringBatteryOptimizations = isIgnoring,
            canRequestIgnore = canRequest,
            isFeatureAvailable = isAvailable,
            explanation = explanation
        )
    }
    
    /**
     * Creates an intent to request battery optimization exemption
     * @return Intent for ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS or null if not available
     */
    @RequiresApi(Build.VERSION_CODES.M)
    fun createBatteryOptimizationExemptionIntent(): Intent? {
        return if (canRequestBatteryOptimizationExemption()) {
            try {
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }
    
    /**
     * Creates an intent to open the device's battery optimization settings
     * @return Intent for battery optimization settings or general application settings as fallback
     */
    fun createBatteryOptimizationSettingsIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                // Try to open the ignore battery optimization settings
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
            } catch (e: Exception) {
                // Fallback to general battery optimization settings
                try {
                    Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS)
                } catch (e2: Exception) {
                    // Final fallback to application settings
                    createApplicationSettingsIntent()
                }
            }
        } else {
            // For older versions, go to application settings
            createApplicationSettingsIntent()
        }
    }
    
    /**
     * Creates an intent to open the app's application info settings
     * @return Intent for application info settings
     */
    private fun createApplicationSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
        }
    }
    
    /**
     * Checks if the device manufacturer has custom battery optimization settings
     * @return true if custom settings are detected, false otherwise
     */
    fun hasCustomBatteryOptimization(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") -> true
            manufacturer.contains("huawei") -> true
            manufacturer.contains("honor") -> true
            manufacturer.contains("oppo") -> true
            manufacturer.contains("vivo") -> true
            manufacturer.contains("oneplus") -> true
            manufacturer.contains("samsung") -> true
            manufacturer.contains("lg") -> true
            manufacturer.contains("sony") -> true
            else -> false
        }
    }
    
    /**
     * Gets manufacturer-specific guidance for battery optimization
     * @return String with manufacturer-specific instructions
     */
    fun getManufacturerSpecificGuidance(): String {
        val manufacturer = Build.MANUFACTURER.lowercase()
        return when {
            manufacturer.contains("xiaomi") -> 
                "For Xiaomi devices: Go to Settings → Apps → Manage apps → WhisperTop → Battery saver → No restrictions"
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> 
                "For Huawei/Honor devices: Go to Settings → Apps → WhisperTop → Battery → Startup manager and enable manual startup"
            manufacturer.contains("oppo") -> 
                "For OPPO devices: Go to Settings → Battery → Power Consumption Protection → WhisperTop → Allow background activity"
            manufacturer.contains("vivo") -> 
                "For Vivo devices: Go to Settings → Battery → Background App Refresh → WhisperTop → Allow"
            manufacturer.contains("oneplus") -> 
                "For OnePlus devices: Go to Settings → Battery → Battery Optimization → WhisperTop → Don't optimize"
            manufacturer.contains("samsung") -> 
                "For Samsung devices: Go to Settings → Apps → WhisperTop → Battery → Optimize battery usage → Turn off"
            else -> 
                "Please check your device's battery optimization settings and whitelist WhisperTop for reliable background operation"
        }
    }
}