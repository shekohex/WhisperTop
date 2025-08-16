package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.data.models.toEntity
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsRepositoryPrivacyTest {

    private val mockPreferencesDataSource = MockPreferencesDataSource()
    private val repository: SettingsRepository = SettingsRepositoryImpl(mockPreferencesDataSource)

    @Test
    fun `clearAllData should reset settings to defaults`() = runTest {
        // Set up some initial settings
        val customSettings = AppSettings(
            apiKey = "sk-test-key",
            selectedModel = "gpt-4",
            enableUsageAnalytics = true,
            enableApiCallLogging = true,
            autoCleanupTempFiles = false,
            tempFileRetentionDays = 14
        )
        repository.updateSettings(customSettings)
        
        // Verify settings were saved
        val savedSettings = repository.getSettings()
        assertEquals("sk-test-key", savedSettings.apiKey)
        assertTrue(savedSettings.enableUsageAnalytics)
        
        // Clear all data
        val result = repository.clearAllData()
        assertTrue(result is Result.Success)
        
        // Verify settings were reset to defaults
        val resetSettings = repository.getSettings()
        assertEquals("", resetSettings.apiKey)
        assertEquals("whisper-1", resetSettings.selectedModel)
        assertFalse(resetSettings.enableUsageAnalytics)
        assertFalse(resetSettings.enableApiCallLogging)
        assertTrue(resetSettings.autoCleanupTempFiles)
        assertEquals(7, resetSettings.tempFileRetentionDays)
    }
    
    @Test
    fun `clearAllData should also clear last recording`() = runTest {
        // Set up a last recording
        val audioFile = AudioFileEntity(
            path = "/test/recording.wav",
            durationMs = 5000L,
            sizeBytes = 1024L,
            createdAt = 1234567890L
        )
        mockPreferencesDataSource.saveLastRecording(audioFile)
        
        // Clear all data
        repository.clearAllData()
        
        // Verify last recording was cleared
        val lastRecording = mockPreferencesDataSource.getLastRecording()
        assertEquals(null, lastRecording)
    }
    
    @Test
    fun `cleanupTemporaryFiles should clear last recording only`() = runTest {
        // Set up settings and recording
        val settings = AppSettings(apiKey = "sk-test-key")
        repository.updateSettings(settings)
        
        val audioFile = AudioFileEntity(
            path = "/test/recording.wav",
            durationMs = 5000L,
            sizeBytes = 1024L,
            createdAt = 1234567890L
        )
        mockPreferencesDataSource.saveLastRecording(audioFile)
        
        // Cleanup temporary files
        val result = repository.cleanupTemporaryFiles()
        assertTrue(result is Result.Success)
        
        // Verify settings were preserved
        val preservedSettings = repository.getSettings()
        assertEquals("sk-test-key", preservedSettings.apiKey)
        
        // Verify last recording was cleared
        val lastRecording = mockPreferencesDataSource.getLastRecording()
        assertEquals(null, lastRecording)
    }
    
    @Test
    fun `privacy settings updates should work correctly`() = runTest {
        val initialSettings = AppSettings()
        repository.updateSettings(initialSettings)
        
        // Update privacy settings
        val updatedSettings = initialSettings.copy(
            enableUsageAnalytics = true,
            enableApiCallLogging = true,
            autoCleanupTempFiles = false,
            tempFileRetentionDays = 14
        )
        
        val result = repository.updateSettings(updatedSettings)
        assertTrue(result is Result.Success)
        
        // Verify privacy settings were saved
        val savedSettings = repository.getSettings()
        assertTrue(savedSettings.enableUsageAnalytics)
        assertTrue(savedSettings.enableApiCallLogging)
        assertFalse(savedSettings.autoCleanupTempFiles)
        assertEquals(14, savedSettings.tempFileRetentionDays)
    }
    
    @Test
    fun `settings flow should reflect privacy changes`() = runTest {
        // Initial settings
        val initialSettings = AppSettings(enableUsageAnalytics = false)
        repository.updateSettings(initialSettings)
        
        // Get initial flow value
        val flowSettings1 = repository.settings.first()
        assertFalse(flowSettings1.enableUsageAnalytics)
        
        // Update privacy setting
        val updatedSettings = initialSettings.copy(enableUsageAnalytics = true)
        repository.updateSettings(updatedSettings)
        
        // Verify flow reflects the change
        val flowSettings2 = repository.settings.first()
        assertTrue(flowSettings2.enableUsageAnalytics)
    }
}

// Mock implementation for testing
private class MockPreferencesDataSource : PreferencesDataSource {
    private val _settings = MutableStateFlow(AppSettingsEntity())
    private var _lastRecording: AudioFileEntity? = null

    override suspend fun getSettings(): AppSettingsEntity {
        return _settings.value
    }

    override suspend fun saveSettings(settings: AppSettingsEntity) {
        _settings.value = settings
    }

    override fun getSettingsFlow(): Flow<AppSettingsEntity> {
        return _settings
    }

    override suspend fun getLastRecording(): AudioFileEntity? {
        return _lastRecording
    }

    override suspend fun saveLastRecording(audioFile: AudioFileEntity) {
        _lastRecording = audioFile
    }

    override suspend fun clearLastRecording() {
        _lastRecording = null
    }
}