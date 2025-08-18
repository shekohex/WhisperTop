package me.shadykhalifa.whispertop.data.services

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.managers.AndroidSystemSettingsProvider
import me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService

class TextInsertionServiceImpl(
    private val context: Context
) : TextInsertionService {
    
    companion object {
        private const val TAG = "TextInsertionServiceImpl"
    }
    
    private val systemSettingsProvider = AndroidSystemSettingsProvider(context)
    
    override suspend fun insertText(text: String): Boolean {
        return withContext(Dispatchers.Main) {
            Log.d(TAG, "insertText called, performing diagnostics...")
            
            // Diagnostic checks
            val accessibilityEnabled = systemSettingsProvider.isAccessibilityEnabled()
            val enabledServices = systemSettingsProvider.getEnabledAccessibilityServices()
            val serviceInstance = WhisperTopAccessibilityService.getInstance()
            
            Log.d(TAG, "Accessibility diagnostics:")
            Log.d(TAG, "  - System accessibility enabled: $accessibilityEnabled")
            Log.d(TAG, "  - Enabled services: $enabledServices")
            Log.d(TAG, "  - WhisperTop service instance: ${if (serviceInstance != null) "available" else "null"}")
            
            if (!accessibilityEnabled) {
                Log.w(TAG, "System accessibility is disabled - user needs to enable accessibility services")
                return@withContext false
            }
            
            val isOurServiceEnabled = enabledServices?.contains("me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService") == true
            Log.d(TAG, "  - WhisperTop service enabled in settings: $isOurServiceEnabled")
            
            if (!isOurServiceEnabled) {
                Log.w(TAG, "WhisperTop accessibility service is not enabled in system settings")
                return@withContext false
            }
            
            if (serviceInstance == null) {
                Log.w(TAG, "Service is enabled in settings but instance is null - service may be crashed or not started")
                return@withContext false
            }
            
            Log.d(TAG, "All accessibility checks passed, calling insertText")
            val result = serviceInstance.insertText(text)
            Log.d(TAG, "Accessibility service insertText returned: $result")
            result
        }
    }
    
    override fun isServiceAvailable(): Boolean {
        val isRunning = WhisperTopAccessibilityService.isServiceRunning()
        val accessibilityEnabled = systemSettingsProvider.isAccessibilityEnabled()
        val enabledServices = systemSettingsProvider.getEnabledAccessibilityServices()
        val isOurServiceEnabled = enabledServices?.contains("me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService") == true
        
        val isAvailable = isRunning && accessibilityEnabled && isOurServiceEnabled
        
        Log.d(TAG, "isServiceAvailable diagnostics:")
        Log.d(TAG, "  - Service instance running: $isRunning")
        Log.d(TAG, "  - System accessibility enabled: $accessibilityEnabled")  
        Log.d(TAG, "  - Our service enabled: $isOurServiceEnabled")
        Log.d(TAG, "  - Overall available: $isAvailable")
        
        return isAvailable
    }
}