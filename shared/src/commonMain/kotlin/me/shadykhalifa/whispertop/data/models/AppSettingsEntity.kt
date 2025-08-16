package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.Serializable
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.LanguagePreference
import me.shadykhalifa.whispertop.domain.models.Language

@Serializable
data class AppSettingsEntity(
    val apiKey: String = "",
    val selectedModel: String = "whisper-1",
    val customModels: List<String> = emptyList(),
    val modelPreferences: Map<String, String> = emptyMap(), // Model ID -> Use case preference
    val language: String? = null,
    val autoDetectLanguage: Boolean = true,
    val languagePreference: LanguagePreference = LanguagePreference(),
    val theme: String = "System",
    val enableHapticFeedback: Boolean = true,
    val enableBatteryOptimization: Boolean = false
)

fun AppSettingsEntity.toDomain(): AppSettings {
    return AppSettings(
        apiKey = apiKey,
        selectedModel = selectedModel,
        customModels = customModels,
        modelPreferences = modelPreferences,
        language = language,
        autoDetectLanguage = autoDetectLanguage,
        languagePreference = languagePreference,
        theme = when (theme) {
            "Light" -> Theme.Light
            "Dark" -> Theme.Dark
            else -> Theme.System
        },
        enableHapticFeedback = enableHapticFeedback,
        enableBatteryOptimization = enableBatteryOptimization
    )
}

fun AppSettings.toEntity(): AppSettingsEntity {
    return AppSettingsEntity(
        apiKey = apiKey,
        selectedModel = selectedModel,
        customModels = customModels,
        modelPreferences = modelPreferences,
        language = language,
        autoDetectLanguage = autoDetectLanguage,
        languagePreference = languagePreference,
        theme = theme.name,
        enableHapticFeedback = enableHapticFeedback,
        enableBatteryOptimization = enableBatteryOptimization
    )
}