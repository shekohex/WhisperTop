package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.repositories.SettingsRepositoryImpl
import me.shadykhalifa.whispertop.data.models.AudioFileEntity
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.models.DefaultDashboardMetrics
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsRepositoryPrivacyTestSimple {

    private val mockPreferencesDataSource = object : PreferencesDataSource {
        private val _settings = MutableStateFlow(AppSettingsEntity())
        private var _lastRecording: AudioFileEntity? = null

        override suspend fun getSettings(): AppSettingsEntity = _settings.value
        override suspend fun saveSettings(settings: AppSettingsEntity) {
            _settings.value = settings
        }
        override fun getSettingsFlow(): Flow<AppSettingsEntity> = _settings
        override suspend fun getLastRecording(): AudioFileEntity? = _lastRecording
        override suspend fun saveLastRecording(audioFile: AudioFileEntity) {
            _lastRecording = audioFile
        }
        override suspend fun clearLastRecording() {
            _lastRecording = null
        }
    }
    
    private val mockSecurePreferencesRepository = object : SecurePreferencesRepository {
        override suspend fun saveApiKey(apiKey: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getApiKey(): Result<String?> = Result.Success(null)
        override suspend fun clearApiKey(): Result<Unit> = Result.Success(Unit)
        override suspend fun hasApiKey(): Result<Boolean> = Result.Success(false)
        override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> = Result.Success(Unit)
        override suspend fun getApiEndpoint(): Result<String> = Result.Success("https://api.openai.com/v1/")
        override fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean): Boolean = true
        override suspend fun saveWpm(wpm: Int): Result<Unit> = Result.Success(Unit)
        override suspend fun getWpm(): Result<Int> = Result.Success(36)
        override suspend fun saveWpmOnboardingCompleted(completed: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun isWpmOnboardingCompleted(): Result<Boolean> = Result.Success(false)
        override fun validateWpm(wpm: Int): Boolean = wpm in 1..300
        
        // Statistics preferences
        override suspend fun saveStatisticsEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun getStatisticsEnabled(): Result<Boolean> = Result.Success(true)
        override suspend fun saveHistoryRetentionDays(days: Int): Result<Unit> = Result.Success(Unit)
        override suspend fun getHistoryRetentionDays(): Result<Int> = Result.Success(30)
        override suspend fun saveExportFormat(format: ExportFormat): Result<Unit> = Result.Success(Unit)
        override suspend fun getExportFormat(): Result<ExportFormat> = Result.Success(ExportFormat.JSON)
        override suspend fun saveDashboardMetricsVisible(metrics: Set<String>): Result<Unit> = Result.Success(Unit)
        override suspend fun getDashboardMetricsVisible(): Result<Set<String>> = Result.Success(DefaultDashboardMetrics.ESSENTIAL_METRICS)
        override suspend fun saveChartTimeRange(range: ChartTimeRange): Result<Unit> = Result.Success(Unit)
        override suspend fun getChartTimeRange(): Result<ChartTimeRange> = Result.Success(ChartTimeRange.DAYS_14)
        override suspend fun saveNotificationsEnabled(enabled: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun getNotificationsEnabled(): Result<Boolean> = Result.Success(true)
        override suspend fun saveDataPrivacyMode(mode: DataPrivacyMode): Result<Unit> = Result.Success(Unit)
        override suspend fun getDataPrivacyMode(): Result<DataPrivacyMode> = Result.Success(DataPrivacyMode.FULL)
        override suspend fun saveAllowDataImport(allow: Boolean): Result<Unit> = Result.Success(Unit)
        override suspend fun getAllowDataImport(): Result<Boolean> = Result.Success(true)
        override fun validateHistoryRetentionDays(days: Int): Boolean = days in 1..365
    }
    
    private val repository: SettingsRepository = SettingsRepositoryImpl(
        mockPreferencesDataSource, 
        mockSecurePreferencesRepository
    )

    @Test
    fun `clearAllData should reset settings to defaults`() = runTest {
        // Set up some initial settings
        val customSettings = AppSettings(
            apiKey = "test-api-key",
            selectedModel = "gpt-4",
            enableUsageAnalytics = true,
            enableApiCallLogging = true,
            autoCleanupTempFiles = false,
            tempFileRetentionDays = 14
        )
        repository.updateSettings(customSettings)
        
        // Verify settings were saved
        val savedSettings = repository.getSettings()
        assertEquals("test-api-key", savedSettings.apiKey)
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
    fun `cleanupTemporaryFiles should preserve settings`() = runTest {
        // Set up settings
        val settings = AppSettings(apiKey = "test-api-key")
        repository.updateSettings(settings)
        
        // Cleanup temporary files
        val result = repository.cleanupTemporaryFiles()
        assertTrue(result is Result.Success)
        
        // Verify settings were preserved
        val preservedSettings = repository.getSettings()
        assertEquals("test-api-key", preservedSettings.apiKey)
    }
}