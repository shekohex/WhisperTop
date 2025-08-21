package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class TranscriptionStatistics(
    val totalCount: Long,
    val totalDuration: Float?,
    val averageConfidence: Float?,
    val mostUsedLanguage: String?,
    val mostUsedModel: String?,
    val dailyTranscriptionCount: Long
)