package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.data.models.toEntity
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result

class SettingsRepositoryImpl(
    private val preferencesDataSource: PreferencesDataSource,
    private val securePreferencesRepository: SecurePreferencesRepository
) : BaseRepository(), SettingsRepository {

    override val settings: Flow<AppSettings> = preferencesDataSource.getSettingsFlow()
        .map { it.toDomain() }

    override suspend fun getSettings(): AppSettings {
        return preferencesDataSource.getSettings().toDomain()
    }

    override suspend fun updateApiKey(apiKey: String): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(apiKey = apiKey.trim())
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateSelectedModel(model: String): Result<Unit> = execute {
        if (model.isBlank()) {
            throw IllegalArgumentException("Model cannot be empty")
        }
        
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(selectedModel = model)
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateLanguage(language: String?): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(language = language?.takeIf { it.isNotBlank() })
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateTheme(theme: Theme): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(theme = theme.name)
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateSettings(settings: AppSettings): Result<Unit> = execute {
        if (settings.selectedModel.isBlank()) {
            throw IllegalArgumentException("Selected model cannot be empty")
        }
        
        preferencesDataSource.saveSettings(settings.toEntity())
    }

    override suspend fun clearApiKey(): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(apiKey = "")
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun clearAllData(): Result<Unit> = execute {
        val defaultSettings = AppSettings()
        preferencesDataSource.saveSettings(defaultSettings.toEntity())
        preferencesDataSource.clearLastRecording()
    }

    override suspend fun updateBaseUrl(baseUrl: String): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val cleanUrl = baseUrl.trim().let { 
            if (it.endsWith("/")) it else "$it/"
        }
        val updatedSettings = currentSettings.copy(
            baseUrl = cleanUrl,
            isCustomEndpoint = !cleanUrl.contains("api.openai.com")
        )
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateCustomEndpoint(isCustom: Boolean): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(isCustomEndpoint = isCustom)
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateCustomPrompt(prompt: String?): Result<Unit> = execute {
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(customPrompt = prompt?.takeIf { it.isNotBlank() })
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun updateTemperature(temperature: Float): Result<Unit> = execute {
        if (temperature < 0.0f || temperature > 2.0f) {
            throw IllegalArgumentException("Temperature must be between 0.0 and 2.0")
        }
        
        val currentSettings = preferencesDataSource.getSettings()
        val updatedSettings = currentSettings.copy(temperature = temperature)
        preferencesDataSource.saveSettings(updatedSettings)
    }

    override suspend fun cleanupTemporaryFiles(): Result<Unit> = execute {
        preferencesDataSource.clearLastRecording()
    }
    
    override suspend fun updateWordsPerMinute(wpm: Int): Result<Unit> {
        return securePreferencesRepository.saveWpm(wpm)
    }
    
    override suspend fun updateWpmOnboardingCompleted(completed: Boolean): Result<Unit> {
        return securePreferencesRepository.saveWpmOnboardingCompleted(completed)
    }
    
    override suspend fun getWordsPerMinute(): Int {
        return when (val result = securePreferencesRepository.getWpm()) {
            is Result.Success -> result.data
            is Result.Error -> 36 // Return default on error
            is Result.Loading -> 36 // Return default during loading
        }
    }
    
    override suspend fun isWpmOnboardingCompleted(): Boolean {
        return when (val result = securePreferencesRepository.isWpmOnboardingCompleted()) {
            is Result.Success -> result.data
            is Result.Error -> false // Return false on error
            is Result.Loading -> false // Return false during loading
        }
    }
    
    // Statistics Preferences Implementation
    
    override suspend fun updateStatisticsEnabled(enabled: Boolean): Result<Unit> {
        return securePreferencesRepository.saveStatisticsEnabled(enabled)
    }
    
    override suspend fun updateHistoryRetentionDays(days: Int): Result<Unit> {
        return securePreferencesRepository.saveHistoryRetentionDays(days)
    }
    
    override suspend fun updateExportFormat(format: ExportFormat): Result<Unit> {
        return securePreferencesRepository.saveExportFormat(format)
    }
    
    override suspend fun updateDashboardMetricsVisible(metrics: Set<String>): Result<Unit> {
        return securePreferencesRepository.saveDashboardMetricsVisible(metrics)
    }
    
    override suspend fun updateChartTimeRange(range: ChartTimeRange): Result<Unit> {
        return securePreferencesRepository.saveChartTimeRange(range)
    }
    
    override suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit> {
        return securePreferencesRepository.saveNotificationsEnabled(enabled)
    }
    
    override suspend fun updateDataPrivacyMode(mode: DataPrivacyMode): Result<Unit> {
        return securePreferencesRepository.saveDataPrivacyMode(mode)
    }
    
    override suspend fun updateAllowDataImport(allow: Boolean): Result<Unit> {
        return securePreferencesRepository.saveAllowDataImport(allow)
    }
}