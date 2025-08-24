package me.shadykhalifa.whispertop.domain.models

import me.shadykhalifa.whispertop.domain.models.LanguagePreference
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.models.DefaultDashboardMetrics

data class AppSettings(
    val apiKey: String = "",
    val selectedModel: String = "whisper-1",
    val customModels: List<String> = emptyList(),
    val modelPreferences: Map<String, String> = emptyMap(),
    val baseUrl: String = "https://api.openai.com/v1/",
    val isCustomEndpoint: Boolean = false,
    val language: String? = null,
    val autoDetectLanguage: Boolean = true,
    val languagePreference: LanguagePreference = LanguagePreference(),
    // Transcription customization settings
    val customPrompt: String? = null,
    val temperature: Float = 0.0f,
    val theme: Theme = Theme.System,
    val enableHapticFeedback: Boolean = true,
    val enableBatteryOptimization: Boolean = false,
    val enableUsageAnalytics: Boolean = false,
    val enableApiCallLogging: Boolean = false,
    val autoCleanupTempFiles: Boolean = true,
    val tempFileRetentionDays: Int = 7,
    // Logging settings
    val logLevel: String = "DEBUG",
    val enableConsoleLogging: Boolean = true,
    val enableFileLogging: Boolean = false,
    val enablePerformanceMetrics: Boolean = true,
    val enableDetailedApiLogging: Boolean = false,
    val maxLogEntries: Int = 1000,
    // Performance monitoring settings
    val enableMemoryMonitoring: Boolean = true,
    val memoryMonitoringIntervalMs: Long = 5000,
    val enablePerformanceWarnings: Boolean = true,
    val performanceThresholds: PerformanceThresholds = PerformanceThresholds(),
    val enableMetricsCollection: Boolean = true,
    val metricsRetentionDays: Int = 30,
    val enableMetricsExport: Boolean = true,
    val autoCleanupMetrics: Boolean = true,
    // Privacy settings
    val enableTranscriptionStorage: Boolean = true,
    val enableAppUsageTracking: Boolean = false,
    val hashTranscriptionText: Boolean = false,
    val maxTranscriptionRetentionDays: Int = 30,
    val autoDeleteTranscriptions: Boolean = true,
    // WPM Configuration settings
    val wordsPerMinute: Int = 36, // Mobile-optimized default based on research
    val wpmOnboardingCompleted: Boolean = false,
    // Statistics and Dashboard Preferences
    val statisticsEnabled: Boolean = true,
    val historyRetentionDays: Int = 90,
    val exportFormat: ExportFormat = ExportFormat.JSON,
    val dashboardMetricsVisible: Set<String> = DefaultDashboardMetrics.ALL_METRICS,
    val chartTimeRange: ChartTimeRange = ChartTimeRange.DAYS_14,
    val notificationsEnabled: Boolean = true,
    val dataPrivacyMode: DataPrivacyMode = DataPrivacyMode.FULL,
    val allowDataImport: Boolean = true
) {
    /**
     * Determines if the current baseUrl is an OpenAI endpoint
     */
    fun isOpenAIEndpoint(): Boolean {
        val isOpenAI = baseUrl.isBlank() || 
                      baseUrl.equals("https://api.openai.com/v1", ignoreCase = true) ||
                      baseUrl.contains("api.openai.com", ignoreCase = true) ||
                      baseUrl.contains("openai.azure.com", ignoreCase = true) ||
                      baseUrl.contains("oai.azure.com", ignoreCase = true)
        
        println("AppSettings: Endpoint detection - baseUrl='$baseUrl', isOpenAI=$isOpenAI")
        return isOpenAI
    }

    /**
     * Validates the history retention days value
     */
    fun validateHistoryRetentionDays(days: Int): String? {
        return when {
            days < 7 -> "History retention must be at least 7 days"
            days > 365 -> "History retention cannot exceed 365 days (1 year)"
            else -> null
        }
    }

    /**
     * Returns a copy of this AppSettings with validated retention days
     */
    fun withValidatedRetentionDays(days: Int): AppSettings {
        val validDays = days.coerceIn(7, 365)
        return copy(historyRetentionDays = validDays)
    }

    /**
     * Checks if statistics collection is effectively enabled based on privacy settings
     */
    fun isStatisticsCollectionEnabled(): Boolean {
        return statisticsEnabled && dataPrivacyMode != DataPrivacyMode.DISABLED
    }

    /**
     * Gets the effective dashboard metrics based on privacy mode
     */
    fun getEffectiveDashboardMetrics(): Set<String> {
        return when (dataPrivacyMode) {
            DataPrivacyMode.DISABLED -> emptySet()
            DataPrivacyMode.ANONYMIZED -> dashboardMetricsVisible.filter { 
                it != "transcription_text" && it != "detailed_content" 
            }.toSet()
            DataPrivacyMode.FULL -> dashboardMetricsVisible
        }
    }
}

enum class Theme {
    Light, Dark, System
}

enum class WhisperModel(val id: String, val displayName: String, val supportsLanguageDetection: Boolean = true) {
    WHISPER_1("whisper-1", "Whisper v1", true),
    WHISPER_LARGE_V3("whisper-large-v3", "Whisper Large v3", true),
    WHISPER_LARGE_V3_TURBO("whisper-large-v3-turbo", "Whisper Large v3 Turbo", true),
    GPT_4O_TRANSCRIBE("gpt-4o-transcribe", "GPT-4o Transcribe", true),
    GPT_4O_MINI_TRANSCRIBE("gpt-4o-mini-transcribe", "GPT-4o Mini Transcribe", true);

    companion object {
        /**
         * Find model by ID
         */
        fun fromId(id: String): WhisperModel? = entries.find { it.id == id }

        /**
         * Get recommended models for language detection
         */
        fun getRecommendedModels(): List<WhisperModel> = listOf(
            GPT_4O_TRANSCRIBE, GPT_4O_MINI_TRANSCRIBE, WHISPER_1
        )
    }
}