package me.shadykhalifa.whispertop.data.models

import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppSettingsEntityPrivacyTest {

    @Test
    fun `default AppSettingsEntity should have conservative privacy defaults`() {
        val entity = AppSettingsEntity()
        
        assertFalse(entity.enableUsageAnalytics, "Usage analytics should be disabled by default")
        assertFalse(entity.enableApiCallLogging, "API call logging should be disabled by default")
        assertTrue(entity.autoCleanupTempFiles, "Auto cleanup should be enabled by default")
        assertEquals(7, entity.tempFileRetentionDays, "Default retention should be 7 days")
    }
    
    @Test
    fun `toDomain should correctly map privacy fields`() {
        val entity = AppSettingsEntity(
            apiKey = "sk-test-key",
            selectedModel = "whisper-1",
            theme = "Dark",
            enableUsageAnalytics = true,
            enableApiCallLogging = true,
            autoCleanupTempFiles = false,
            tempFileRetentionDays = 14
        )
        
        val domain = entity.toDomain()
        
        assertEquals("sk-test-key", domain.apiKey)
        assertEquals("whisper-1", domain.selectedModel)
        assertEquals(Theme.Dark, domain.theme)
        assertTrue(domain.enableUsageAnalytics)
        assertTrue(domain.enableApiCallLogging)
        assertFalse(domain.autoCleanupTempFiles)
        assertEquals(14, domain.tempFileRetentionDays)
    }
    
    @Test
    fun `toEntity should correctly map privacy fields`() {
        val domain = AppSettings(
            apiKey = "sk-test-key",
            selectedModel = "gpt-4",
            theme = Theme.Light,
            enableUsageAnalytics = true,
            enableApiCallLogging = false,
            autoCleanupTempFiles = true,
            tempFileRetentionDays = 21
        )
        
        val entity = domain.toEntity()
        
        assertEquals("sk-test-key", entity.apiKey)
        assertEquals("gpt-4", entity.selectedModel)
        assertEquals("Light", entity.theme)
        assertTrue(entity.enableUsageAnalytics)
        assertFalse(entity.enableApiCallLogging)
        assertTrue(entity.autoCleanupTempFiles)
        assertEquals(21, entity.tempFileRetentionDays)
    }
    
    @Test
    fun `round trip conversion should preserve all privacy fields`() {
        val originalDomain = AppSettings(
            apiKey = "sk-original-key",
            selectedModel = "whisper-large-v3",
            enableUsageAnalytics = true,
            enableApiCallLogging = true,
            autoCleanupTempFiles = false,
            tempFileRetentionDays = 30
        )
        
        val entity = originalDomain.toEntity()
        val convertedDomain = entity.toDomain()
        
        assertEquals(originalDomain.enableUsageAnalytics, convertedDomain.enableUsageAnalytics)
        assertEquals(originalDomain.enableApiCallLogging, convertedDomain.enableApiCallLogging)
        assertEquals(originalDomain.autoCleanupTempFiles, convertedDomain.autoCleanupTempFiles)
        assertEquals(originalDomain.tempFileRetentionDays, convertedDomain.tempFileRetentionDays)
        
        // Verify other fields are also preserved
        assertEquals(originalDomain.apiKey, convertedDomain.apiKey)
        assertEquals(originalDomain.selectedModel, convertedDomain.selectedModel)
    }
}