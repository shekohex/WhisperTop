package me.shadykhalifa.whispertop.data.database.mappers

import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import me.shadykhalifa.whispertop.domain.models.UserStatistics

fun UserStatisticsEntity.toDomain(): UserStatistics {
    return UserStatistics(
        id = id,
        totalTranscriptions = totalTranscriptions,
        totalDurationMinutes = totalDurationMinutes,
        totalWordsTranscribed = totalWordsTranscribed,
        averageConfidence = averageConfidence,
        dailyUsageCount = dailyUsageCount,
        lastUsedDate = lastUsedDate,
        mostUsedLanguage = mostUsedLanguage,
        mostUsedModel = mostUsedModel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun UserStatistics.toEntity(): UserStatisticsEntity {
    return UserStatisticsEntity(
        id = id,
        totalTranscriptions = totalTranscriptions,
        totalDurationMinutes = totalDurationMinutes,
        totalWordsTranscribed = totalWordsTranscribed,
        averageConfidence = averageConfidence,
        dailyUsageCount = dailyUsageCount,
        lastUsedDate = lastUsedDate,
        mostUsedLanguage = mostUsedLanguage,
        mostUsedModel = mostUsedModel,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}