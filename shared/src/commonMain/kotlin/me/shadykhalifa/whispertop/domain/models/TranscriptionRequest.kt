package me.shadykhalifa.whispertop.domain.models

data class TranscriptionRequest(
    val audioFile: AudioFile,
    val language: String? = null,
    val model: String = "whisper-1"
)