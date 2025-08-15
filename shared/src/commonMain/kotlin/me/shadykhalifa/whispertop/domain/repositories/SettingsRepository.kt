package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.utils.Result

interface SettingsRepository {
    val settings: Flow<AppSettings>
    
    suspend fun getSettings(): AppSettings
    suspend fun updateApiKey(apiKey: String): Result<Unit>
    suspend fun updateSelectedModel(model: String): Result<Unit>
    suspend fun updateLanguage(language: String?): Result<Unit>
    suspend fun updateTheme(theme: me.shadykhalifa.whispertop.domain.models.Theme): Result<Unit>
    suspend fun updateSettings(settings: AppSettings): Result<Unit>
    suspend fun clearApiKey(): Result<Unit>
}