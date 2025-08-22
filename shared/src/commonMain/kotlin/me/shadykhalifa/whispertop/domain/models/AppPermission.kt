package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
enum class AppPermission(
    val displayName: String,
    val description: String
) {
    RECORD_AUDIO(
        displayName = "Record Audio",
        description = "Required to capture voice for transcription"
    ),
    SYSTEM_ALERT_WINDOW(
        displayName = "Display over other apps",
        description = "Required to show the floating microphone button over other apps"
    ),
    ACCESSIBILITY_SERVICE(
        displayName = "Accessibility Service",
        description = "Required to automatically insert transcribed text into input fields"
    )
}