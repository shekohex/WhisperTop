package me.shadykhalifa.whispertop.domain.models

data class AudioFile(
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val sessionId: String? = null
) {
    val duration: Long get() = durationMs
}