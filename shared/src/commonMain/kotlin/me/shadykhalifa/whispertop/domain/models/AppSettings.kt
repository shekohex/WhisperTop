package me.shadykhalifa.whispertop.domain.models

import me.shadykhalifa.whispertop.domain.models.LanguagePreference

data class AppSettings(
    val apiKey: String = "",
    val selectedModel: String = "whisper-1",
    val customModels: List<String> = emptyList(),
    val modelPreferences: Map<String, String> = emptyMap(),
    val language: String? = null,
    val autoDetectLanguage: Boolean = true,
    val languagePreference: LanguagePreference = LanguagePreference(),
    val theme: Theme = Theme.System,
    val enableHapticFeedback: Boolean = true,
    val enableBatteryOptimization: Boolean = false
)

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