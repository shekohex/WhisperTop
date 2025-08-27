package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.database.dao.UserStatisticsDao
import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import me.shadykhalifa.whispertop.domain.models.UserStatistics
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.utils.Result

class UserStatisticsRepositoryImpl(
    private val userStatisticsDao: UserStatisticsDao,
    private val transcriptionHistoryDao: TranscriptionHistoryDao
) : BaseRepository(), UserStatisticsRepository {

    override suspend fun createUserStatistics(userId: String): Result<Unit> = execute {
        try {
            val currentTime = System.currentTimeMillis()
            val entity = UserStatisticsEntity(
                id = userId,
                totalTranscriptions = 0L,
                totalDuration = 0f,
                averageAccuracy = null,
                dailyUsageCount = 0L,
                mostUsedLanguage = null,
                mostUsedModel = null,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            userStatisticsDao.insertOrUpdate(entity)
        } catch (e: Exception) {
            // If insert fails, try update as fallback
            try {
                val existing = userStatisticsDao.getById(userId)
                if (existing != null) {
                    val currentTime = System.currentTimeMillis()
                    val updatedEntity = existing.copy(
                        totalTranscriptions = existing.totalTranscriptions,
                        totalDuration = existing.totalDuration,
                        averageAccuracy = existing.averageAccuracy,
                        dailyUsageCount = existing.dailyUsageCount,
                        mostUsedLanguage = existing.mostUsedLanguage,
                        mostUsedModel = existing.mostUsedModel,
                        updatedAt = currentTime
                    )
                    userStatisticsDao.update(updatedEntity)
                } else {
                    throw e // Re-throw if no existing record
                }
            } catch (updateException: Exception) {
                throw e // Throw original exception
            }
        }
    }

    override suspend fun getUserStatistics(userId: String): Result<UserStatistics?> = execute {
        try {
            val entity = userStatisticsDao.getById(userId)
            if (entity != null) {
                entity.toDomainModel()
            } else {
                // If no statistics exist, try to create default ones
                createDefaultStatisticsIfNeeded(userId)
                null // Return null to trigger creation in ViewModel
            }
        } catch (e: Exception) {
            // If database operation fails, return null to trigger fallback
            null
        }
    }

    private suspend fun createDefaultStatisticsIfNeeded(userId: String) {
        try {
            val currentTime = System.currentTimeMillis()
            val entity = UserStatisticsEntity(
                id = userId,
                totalTranscriptions = 0L,
                totalDuration = 0f,
                averageAccuracy = null,
                dailyUsageCount = 0L,
                mostUsedLanguage = null,
                mostUsedModel = null,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            userStatisticsDao.insert(entity)
        } catch (e: Exception) {
            // Log but don't throw - this is a best-effort operation
            // TODO: Consider implementing proper error reporting/logging
            e.printStackTrace()
        }
    }

    override fun getUserStatisticsFlow(userId: String): Flow<UserStatistics?> {
        return userStatisticsDao.getByIdFlow(userId).map { it?.toDomainModel() }
    }

    override suspend fun updateUserStatistics(statistics: UserStatistics): Result<Unit> = execute {
        userStatisticsDao.update(statistics.toEntity())
    }

    override suspend fun incrementTranscriptionCount(
        userId: String,
        duration: Float,
        dailyUsageCount: Long
    ): Result<Unit> = execute {
        val currentTime = System.currentTimeMillis()
        userStatisticsDao.incrementTranscriptionStats(
            id = userId,
            duration = duration,
            dailyUsageCount = dailyUsageCount,
            updatedAt = currentTime
        )
    }

    override suspend fun updateDerivedStatistics(userId: String): Result<Unit> = execute {
        val accuracy = transcriptionHistoryDao.getAverageConfidence()
        val language = transcriptionHistoryDao.getMostUsedLanguage()?.language
        val model = transcriptionHistoryDao.getMostUsedModel()?.model
        val currentTime = System.currentTimeMillis()

        userStatisticsDao.updateDerivedStats(
            id = userId,
            accuracy = accuracy,
            language = language,
            model = model,
            updatedAt = currentTime
        )
    }

    override suspend fun deleteUserStatistics(userId: String): Result<Unit> = execute {
        userStatisticsDao.deleteById(userId)
    }

    override suspend fun updateDailyAggregatedStats(
        date: LocalDate,
        totalSessions: Int,
        totalWords: Long,
        totalSpeakingTime: Long,
        averageSessionDuration: Double,
        peakUsageHour: Int
    ): Result<Unit> = execute {
        val currentTime = System.currentTimeMillis()
        val userId = "default_user" // Match the user ID used in DashboardViewModel

        // First ensure the user statistics record exists
        val existingStats = userStatisticsDao.getById(userId)
        if (existingStats == null) {
            // Create initial statistics record
            val initialStats = UserStatisticsEntity(
                id = userId,
                totalSessions = 0,
                totalWords = 0L,
                totalSpeakingTimeMs = 0L,
                averageWordsPerMinute = 0.0,
                averageWordsPerSession = 0.0,
                userTypingWpm = 40, // Default typing speed
                totalTranscriptions = 0L,
                totalDuration = 0f,
                averageAccuracy = null,
                dailyUsageCount = 0L,
                mostUsedLanguage = null,
                mostUsedModel = null,
                createdAt = currentTime,
                updatedAt = currentTime
            )
            userStatisticsDao.insert(initialStats)
        }

        // Update with aggregated daily data using the DAO method
        userStatisticsDao.updateDailyAggregatedStats(
            userId = userId,
            totalSessions = totalSessions,
            totalWords = totalWords,
            totalSpeakingTime = totalSpeakingTime,
            currentTime = currentTime
        )
    }

    override suspend fun deleteAllStatistics(): Result<Unit> = execute {
        userStatisticsDao.deleteAll()
    }

    private fun UserStatisticsEntity.toDomainModel(): UserStatistics {
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

    private fun UserStatistics.toEntity(): UserStatisticsEntity {
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
}