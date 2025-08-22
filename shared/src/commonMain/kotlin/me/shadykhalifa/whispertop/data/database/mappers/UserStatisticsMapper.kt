package me.shadykhalifa.whispertop.data.database.mappers

import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import me.shadykhalifa.whispertop.domain.models.UserStatistics

fun UserStatisticsEntity.toDomain(): UserStatistics {
    return UserStatistics(
        id = id,
        totalWords = totalWordsTranscribed,
        totalSessions = 0, // New field - default value
        totalSpeakingTimeMs = (totalDurationMinutes * 60 * 1000).toLong(), // Convert minutes to ms
        averageWordsPerMinute = 0.0, // New field - default value
        averageWordsPerSession = if (totalTranscriptions > 0) totalWordsTranscribed.toDouble() / totalTranscriptions else 0.0,
        userTypingWpm = 0, // New field - default value
        totalTranscriptions = totalTranscriptions,
        totalDuration = totalDurationMinutes,
        averageAccuracy = averageConfidence,
        dailyUsageCount = dailyUsageCount.toLong(),
        mostUsedLanguage = mostUsedLanguage.ifEmpty { null },
        mostUsedModel = mostUsedModel.ifEmpty { null },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun UserStatistics.toEntity(): UserStatisticsEntity {
    return UserStatisticsEntity(
        id = id,
        totalTranscriptions = totalTranscriptions,
        totalDurationMinutes = totalDuration,
        totalWordsTranscribed = totalWords,
        averageConfidence = averageAccuracy ?: 0f,
        dailyUsageCount = dailyUsageCount.toInt(),
        lastUsedDate = "", // Legacy field - could be computed or left empty
        mostUsedLanguage = mostUsedLanguage ?: "",
        mostUsedModel = mostUsedModel ?: "",
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}