package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserStatistics(
    val id: String,
    val totalTranscriptions: Long,
    val totalDuration: Float,
    val averageAccuracy: Float?,
    val dailyUsageCount: Long,
    val mostUsedLanguage: String?,
    val mostUsedModel: String?,
    val createdAt: Long,
    val updatedAt: Long
)