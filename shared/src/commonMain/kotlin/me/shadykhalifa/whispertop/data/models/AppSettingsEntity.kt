package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.Serializable
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme

@Serializable
data class AppSettingsEntity(
    val apiKey: String = "",
    val selectedModel: String = "whisper-1",
    val language: String? = null,
    val autoDetectLanguage: Boolean = true,
    val theme: String = "System",
    val enableHapticFeedback: Boolean = true,
    val enableBatteryOptimization: Boolean = false
)

fun AppSettingsEntity.toDomain(): AppSettings {
    return AppSettings(
        apiKey = apiKey,
        selectedModel = selectedModel,
        language = language,
        autoDetectLanguage = autoDetectLanguage,
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
        language = language,
        autoDetectLanguage = autoDetectLanguage,
        theme = theme.name,
        enableHapticFeedback = enableHapticFeedback,
        enableBatteryOptimization = enableBatteryOptimization
    )
}