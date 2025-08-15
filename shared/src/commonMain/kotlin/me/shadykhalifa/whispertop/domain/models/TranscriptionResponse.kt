package me.shadykhalifa.whispertop.domain.models

data class TranscriptionResponse(
    val text: String,
    val language: String? = null,
    val duration: Float? = null
)