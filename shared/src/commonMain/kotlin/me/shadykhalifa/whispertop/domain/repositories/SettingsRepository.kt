package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.utils.Result

interface SettingsRepository {
    val settings: Flow<AppSettings>
    
    suspend fun getSettings(): AppSettings
    suspend fun updateApiKey(apiKey: String): Result<Unit>
    suspend fun updateSelectedModel(model: String): Result<Unit>
    suspend fun updateLanguage(language: String?): Result<Unit>
    suspend fun updateTheme(theme: me.shadykhalifa.whispertop.domain.models.Theme): Result<Unit>
    suspend fun updateBaseUrl(baseUrl: String): Result<Unit>
    suspend fun updateCustomEndpoint(isCustom: Boolean): Result<Unit>
    suspend fun updateCustomPrompt(prompt: String?): Result<Unit>
    suspend fun updateTemperature(temperature: Float): Result<Unit>
    suspend fun updateSettings(settings: AppSettings): Result<Unit>
    suspend fun clearApiKey(): Result<Unit>
    suspend fun clearAllData(): Result<Unit>
    suspend fun cleanupTemporaryFiles(): Result<Unit>
    
    // WPM Configuration methods
    suspend fun updateWordsPerMinute(wpm: Int): Result<Unit>
    suspend fun updateWpmOnboardingCompleted(completed: Boolean): Result<Unit>
    suspend fun getWordsPerMinute(): Int
    suspend fun isWpmOnboardingCompleted(): Boolean
    
    // Statistics Preferences methods
    suspend fun updateStatisticsEnabled(enabled: Boolean): Result<Unit>
    suspend fun updateHistoryRetentionDays(days: Int): Result<Unit>
    suspend fun updateExportFormat(format: ExportFormat): Result<Unit>
    suspend fun updateDashboardMetricsVisible(metrics: Set<String>): Result<Unit>
    suspend fun updateChartTimeRange(range: ChartTimeRange): Result<Unit>
    suspend fun updateNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun updateDataPrivacyMode(mode: DataPrivacyMode): Result<Unit>
    suspend fun updateAllowDataImport(allow: Boolean): Result<Unit>
}