package me.shadykhalifa.whispertop.ui.overlay

import androidx.compose.ui.graphics.Color

enum class MicButtonState(
    val color: Color,
    val description: String
) {
    IDLE(Color(0xFF9E9E9E), "Microphone idle"),
    RECORDING(Color(0xFFE53E3E), "Recording audio"),
    PROCESSING(Color(0xFF3182CE), "Processing audio")
}