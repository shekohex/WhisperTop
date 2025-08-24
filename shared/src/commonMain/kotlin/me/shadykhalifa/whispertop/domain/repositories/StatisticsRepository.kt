package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.utils.Result

interface StatisticsRepository {
    
    suspend fun getUserStatistics(userId: String): Result<UserStatistics?>
    
    fun getUserStatisticsFlow(userId: String): Flow<UserStatistics?>
    
    suspend fun updateUserStatistics(statistics: UserStatistics): Result<Unit>
    
    suspend fun incrementWordCount(
        userId: String,
        wordsAdded: Int,
        speakingTimeMs: Long
    ): Result<Unit>
    
    suspend fun updateTypingSpeed(
        userId: String,
        typingWpm: Int
    ): Result<Unit>
    
    suspend fun getDailyUsage(
        userId: String,
        date: LocalDate
    ): Result<DailyUsage?>
    
    suspend fun getDailyUsageRange(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Result<List<DailyUsage>>
    
    fun getDailyUsageRangeFlow(
        userId: String,
        startDate: LocalDate,
        endDate: LocalDate
    ): Flow<List<DailyUsage>>
    
    suspend fun recordDailyUsage(
        userId: String,
        date: LocalDate,
        sessionCount: Int,
        wordsTranscribed: Long,
        totalTimeMs: Long
    ): Result<Unit>
    
    suspend fun deleteUserStatistics(userId: String): Result<Unit>
    
    suspend fun deleteAllStatistics(): Result<Unit>
    
    // Performance optimization methods
    suspend fun getDailyStatistics(
        userId: String,
        date: LocalDate
    ): Result<DailyUsage?>
}