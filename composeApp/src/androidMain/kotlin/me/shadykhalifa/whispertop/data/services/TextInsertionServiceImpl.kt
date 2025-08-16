package me.shadykhalifa.whispertop.data.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.service.WhisperTopAccessibilityService

class TextInsertionServiceImpl : TextInsertionService {
    
    override suspend fun insertText(text: String): Boolean {
        return withContext(Dispatchers.Main) {
            val accessibilityService = WhisperTopAccessibilityService.getInstance()
            accessibilityService?.insertText(text) ?: false
        }
    }
    
    override fun isServiceAvailable(): Boolean {
        return WhisperTopAccessibilityService.isServiceRunning()
    }
}