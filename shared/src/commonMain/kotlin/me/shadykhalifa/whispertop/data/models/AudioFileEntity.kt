package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.Serializable
import me.shadykhalifa.whispertop.domain.models.AudioFile

@Serializable
data class AudioFileEntity(
    val path: String,
    val durationMs: Long,
    val sizeBytes: Long,
    val createdAt: Long = 0L
)

fun AudioFileEntity.toDomain(): AudioFile {
    return AudioFile(
        path = path,
        durationMs = durationMs,
        sizeBytes = sizeBytes
    )
}

fun AudioFile.toEntity(): AudioFileEntity {
    return AudioFileEntity(
        path = path,
        durationMs = durationMs,
        sizeBytes = sizeBytes
    )
}