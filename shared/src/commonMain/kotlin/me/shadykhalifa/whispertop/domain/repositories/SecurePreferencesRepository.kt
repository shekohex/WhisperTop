package me.shadykhalifa.whispertop.domain.repositories

import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode

interface SecurePreferencesRepository {
    suspend fun saveApiKey(apiKey: String): Result<Unit>
    suspend fun getApiKey(): Result<String?>
    suspend fun clearApiKey(): Result<Unit>
    suspend fun hasApiKey(): Result<Boolean>
    suspend fun saveApiEndpoint(endpoint: String): Result<Unit>
    suspend fun getApiEndpoint(): Result<String>
    fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean = true): Boolean
    
    // WPM Configuration methods
    suspend fun saveWpm(wpm: Int): Result<Unit>
    suspend fun getWpm(): Result<Int>
    suspend fun saveWpmOnboardingCompleted(completed: Boolean): Result<Unit>
    suspend fun isWpmOnboardingCompleted(): Result<Boolean>
    fun validateWpm(wpm: Int): Boolean
    
    // Statistics Preferences methods
    suspend fun saveStatisticsEnabled(enabled: Boolean): Result<Unit>
    suspend fun getStatisticsEnabled(): Result<Boolean>
    suspend fun saveHistoryRetentionDays(days: Int): Result<Unit>
    suspend fun getHistoryRetentionDays(): Result<Int>
    suspend fun saveExportFormat(format: ExportFormat): Result<Unit>
    suspend fun getExportFormat(): Result<ExportFormat>
    suspend fun saveDashboardMetricsVisible(metrics: Set<String>): Result<Unit>
    suspend fun getDashboardMetricsVisible(): Result<Set<String>>
    suspend fun saveChartTimeRange(range: ChartTimeRange): Result<Unit>
    suspend fun getChartTimeRange(): Result<ChartTimeRange>
    suspend fun saveNotificationsEnabled(enabled: Boolean): Result<Unit>
    suspend fun getNotificationsEnabled(): Result<Boolean>
    suspend fun saveDataPrivacyMode(mode: DataPrivacyMode): Result<Unit>
    suspend fun getDataPrivacyMode(): Result<DataPrivacyMode>
    suspend fun saveAllowDataImport(allow: Boolean): Result<Unit>
    suspend fun getAllowDataImport(): Result<Boolean>
    fun validateHistoryRetentionDays(days: Int): Boolean
}