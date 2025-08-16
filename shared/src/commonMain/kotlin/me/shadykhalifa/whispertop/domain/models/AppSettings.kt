package me.shadykhalifa.whispertop.domain.models

data class AppSettings(
    val apiKey: String = "",
    val selectedModel: String = "whisper-1",
    val customModels: List<String> = emptyList(),
    val modelPreferences: Map<String, String> = emptyMap(),
    val language: String? = null,
    val autoDetectLanguage: Boolean = true,
    val theme: Theme = Theme.System,
    val enableHapticFeedback: Boolean = true,
    val enableBatteryOptimization: Boolean = false
)

enum class Theme {
    Light, Dark, System
}

enum class WhisperModel(val id: String, val displayName: String) {
    WHISPER_1("whisper-1", "Whisper v1"),
    WHISPER_LARGE_V3("whisper-large-v3", "Whisper Large v3"),
    WHISPER_LARGE_V3_TURBO("whisper-large-v3-turbo", "Whisper Large v3 Turbo")
}