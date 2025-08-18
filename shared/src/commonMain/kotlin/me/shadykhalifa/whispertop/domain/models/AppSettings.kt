package me.shadykhalifa.whispertop.domain.models

import me.shadykhalifa.whispertop.domain.models.LanguagePreference

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
    val autoCleanupMetrics: Boolean = true
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