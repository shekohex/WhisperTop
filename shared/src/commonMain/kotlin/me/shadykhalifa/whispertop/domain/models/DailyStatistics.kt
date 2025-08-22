package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class DailyStatistics(
    val date: LocalDate,
    val sessionsCount: Int,
    val successfulSessions: Int,
    val totalWords: Long,
    val totalCharacters: Long,
    val totalSpeakingTimeMs: Long,
    val totalSessionDurationMs: Long,
    val averageSessionDuration: Double,
    val averageSpeakingRate: Double, // words per minute
    val successRate: Double, // percentage
    val peakUsageHour: Int, // 0-23
    val mostUsedApp: String?,
    val errorBreakdown: Map<String, Int>,
    val uniqueAppsUsed: Int
) {
    companion object {
        fun empty(date: LocalDate) = DailyStatistics(
            date = date,
            sessionsCount = 0,
            successfulSessions = 0,
            totalWords = 0L,
            totalCharacters = 0L,
            totalSpeakingTimeMs = 0L,
            totalSessionDurationMs = 0L,
            averageSessionDuration = 0.0,
            averageSpeakingRate = 0.0,
            successRate = 0.0,
            peakUsageHour = 0,
            mostUsedApp = null,
            errorBreakdown = emptyMap(),
            uniqueAppsUsed = 0
        )
    }
    
    val averageWordsPerSession: Double
        get() = if (sessionsCount > 0) totalWords.toDouble() / sessionsCount else 0.0
    
    val failureRate: Double
        get() = 100.0 - successRate
    
    val totalErrorsCount: Int
        get() = errorBreakdown.values.sum()
}