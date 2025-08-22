package me.shadykhalifa.whispertop.data.database.mappers

import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import me.shadykhalifa.whispertop.domain.models.UserStatistics

fun UserStatisticsEntity.toDomain(): UserStatistics {
    return UserStatistics(
        id = id,
        totalWords = totalWords,
        totalSessions = totalSessions,
        totalSpeakingTimeMs = totalSpeakingTimeMs,
        averageWordsPerMinute = averageWordsPerMinute,
        averageWordsPerSession = averageWordsPerSession,
        userTypingWpm = userTypingWpm,
        totalTranscriptions = totalTranscriptions,
        totalDuration = totalDuration,
        averageAccuracy = averageAccuracy,
        dailyUsageCount = dailyUsageCount,
        mostUsedLanguage = mostUsedLanguage,
        mostUsedModel = mostUsedModel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun UserStatistics.toEntity(): UserStatisticsEntity {
    return UserStatisticsEntity(
        id = id,
        totalWords = totalWords,
        totalSessions = totalSessions,
        totalSpeakingTimeMs = totalSpeakingTimeMs,
        averageWordsPerMinute = averageWordsPerMinute,
        averageWordsPerSession = averageWordsPerSession,
        userTypingWpm = userTypingWpm,
        totalTranscriptions = totalTranscriptions,
        totalDuration = totalDuration,
        averageAccuracy = averageAccuracy,
        dailyUsageCount = dailyUsageCount,
        mostUsedLanguage = mostUsedLanguage,
        mostUsedModel = mostUsedModel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}