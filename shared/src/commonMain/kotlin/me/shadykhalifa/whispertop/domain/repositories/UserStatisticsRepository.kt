package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.utils.Result

interface UserStatisticsRepository {
    
    suspend fun createUserStatistics(userId: String): Result<Unit>
    
    suspend fun getUserStatistics(userId: String): Result<UserStatistics?>
    
    fun getUserStatisticsFlow(userId: String): Flow<UserStatistics?>
    
    suspend fun updateUserStatistics(statistics: UserStatistics): Result<Unit>
    
    suspend fun incrementTranscriptionCount(
        userId: String,
        duration: Float,
        dailyUsageCount: Long
    ): Result<Unit>
    
    suspend fun updateDerivedStatistics(userId: String): Result<Unit>
    
    suspend fun deleteUserStatistics(userId: String): Result<Unit>
    
    suspend fun deleteAllStatistics(): Result<Unit>
}