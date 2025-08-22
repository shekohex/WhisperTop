package me.shadykhalifa.whispertop.presentation.models

import me.shadykhalifa.whispertop.domain.models.AudioFile
import kotlin.math.pow

data class AudioFilePresentationModel(
    val path: String,
    val durationText: String,
    val fileSizeText: String,
    val isValid: Boolean,
    val sessionId: String?
)

fun AudioFile.toPresentationModel(): AudioFilePresentationModel {
    return AudioFilePresentationModel(
        path = path,
        durationText = formatDuration(durationMs),
        fileSizeText = formatFileSize(sizeBytes),
        isValid = path.isNotEmpty() && durationMs > 0 && sizeBytes > 0,
        sessionId = sessionId
    )
}

private fun formatDuration(durationMs: Long): String {
    if (durationMs <= 0) return "0:00"
    
    val totalSeconds = (durationMs / 1000).toInt()
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    
    return "${minutes}:${seconds.toString().padStart(2, '0')}"
}

private fun formatFileSize(sizeBytes: Long): String {
    if (sizeBytes <= 0) return "0 B"
    
    val units = arrayOf("B", "KB", "MB", "GB")
    var size = sizeBytes.toDouble()
    var unitIndex = 0
    
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    
    return if (size >= 100) {
        "${size.toInt()} ${units[unitIndex]}"
    } else {
        "%.1f ${units[unitIndex]}".format(size)
    }
}