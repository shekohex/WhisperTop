package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.os.Build
import android.provider.Settings

/**
 * Abstraction layer for Android system settings access.
 * This allows for easier testing by providing mockable interfaces for static method calls.
 */
interface SystemSettingsProvider {
    /**
     * Checks if the app can draw overlays (system alert window permission).
     * @return true if the app has overlay permission, false otherwise
     */
    fun canDrawOverlays(): Boolean
    
    /**
     * Checks if accessibility services are enabled globally.
     * @return true if accessibility is enabled, false otherwise
     */
    fun isAccessibilityEnabled(): Boolean
    
    /**
     * Gets the list of enabled accessibility services.
     * @return comma-separated string of enabled accessibility services, or null if none
     */
    fun getEnabledAccessibilityServices(): String?
}

/**
 * Implementation of SystemSettingsProvider that uses real Android system settings.
 */
class AndroidSystemSettingsProvider(private val context: Context) : SystemSettingsProvider {
    
    override fun canDrawOverlays(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else {
            true // No overlay permission required before API 23
        }
    }
    
    override fun isAccessibilityEnabled(): Boolean {
        return try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED
            ) == 1
        } catch (e: Settings.SettingNotFoundException) {
            false
        }
    }
    
    override fun getEnabledAccessibilityServices(): String? {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
    }
}