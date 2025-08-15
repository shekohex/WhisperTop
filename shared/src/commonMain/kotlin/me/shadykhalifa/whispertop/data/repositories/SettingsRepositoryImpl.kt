package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.data.models.toEntity
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.utils.Result

class SettingsRepositoryImpl(
    private val preferencesDataSource: PreferencesDataSource
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
        if (settings.apiKey.isBlank()) {
            throw IllegalArgumentException("API key cannot be empty")
        }
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
}