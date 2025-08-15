package me.shadykhalifa.whispertop.domain.models

data class AudioFile(
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long
)