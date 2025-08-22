package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import me.shadykhalifa.whispertop.utils.TestConstants

class AppSettingsPrivacyTest {

    @Test
    fun `default privacy settings should be conservative`() {
        val settings = AppSettings()
        
        assertFalse(settings.enableUsageAnalytics, "Usage analytics should be disabled by default")
        assertFalse(settings.enableApiCallLogging, "API call logging should be disabled by default")
        assertTrue(settings.autoCleanupTempFiles, "Auto cleanup should be enabled by default")
        assertEquals(7, settings.tempFileRetentionDays, "Default retention should be 7 days")
    }
    
    @Test
    fun `privacy settings should be configurable`() {
        val settings = AppSettings(
            enableUsageAnalytics = true,
            enableApiCallLogging = true,
            autoCleanupTempFiles = false,
            tempFileRetentionDays = 14
        )
        
        assertTrue(settings.enableUsageAnalytics)
        assertTrue(settings.enableApiCallLogging)
        assertFalse(settings.autoCleanupTempFiles)
        assertEquals(14, settings.tempFileRetentionDays)
    }
    
    @Test
    fun `tempFileRetentionDays should have reasonable bounds`() {
        val settingsMin = AppSettings(tempFileRetentionDays = 1)
        val settingsMax = AppSettings(tempFileRetentionDays = 30)
        
        assertEquals(1, settingsMin.tempFileRetentionDays)
        assertEquals(30, settingsMax.tempFileRetentionDays)
    }
    
    @Test
    fun `copy with privacy settings should preserve other settings`() {
        val originalSettings = AppSettings(
            apiKey = TestConstants.MOCK_API_KEY,
            selectedModel = "whisper-1",
            theme = Theme.Dark,
            enableHapticFeedback = false
        )
        
        val updatedSettings = originalSettings.copy(
            enableUsageAnalytics = true,
            autoCleanupTempFiles = false
        )
        
        // Original settings should be preserved
        assertEquals(TestConstants.MOCK_API_KEY, updatedSettings.apiKey)
        assertEquals("whisper-1", updatedSettings.selectedModel)
        assertEquals(Theme.Dark, updatedSettings.theme)
        assertFalse(updatedSettings.enableHapticFeedback)
        
        // Privacy settings should be updated
        assertTrue(updatedSettings.enableUsageAnalytics)
        assertFalse(updatedSettings.autoCleanupTempFiles)
    }
}