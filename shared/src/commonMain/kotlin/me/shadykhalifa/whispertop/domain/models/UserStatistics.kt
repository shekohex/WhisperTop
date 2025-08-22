package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class UserStatistics(
    val id: String,
    val totalWords: Long,
    val totalSessions: Int,
    val totalSpeakingTimeMs: Long,
    val averageWordsPerMinute: Double,
    val averageWordsPerSession: Double,
    val userTypingWpm: Int,
    val totalTranscriptions: Long,
    val totalDuration: Float,
    val averageAccuracy: Float?,
    val dailyUsageCount: Long,
    val mostUsedLanguage: String?,
    val mostUsedModel: String?,
    val createdAt: Long,
    val updatedAt: Long
)

interface UserStatisticsData {
    val id: String
    val totalWords: Long
    val totalSessions: Int
    val totalSpeakingTimeMs: Long
    val averageWordsPerMinute: Double
    val averageWordsPerSession: Double
    val userTypingWpm: Int
    val totalTranscriptions: Long
    val totalDuration: Float
    val averageAccuracy: Float?
    val dailyUsageCount: Long
    val mostUsedLanguage: String?
    val mostUsedModel: String?
    val createdAt: Long
    val updatedAt: Long
}