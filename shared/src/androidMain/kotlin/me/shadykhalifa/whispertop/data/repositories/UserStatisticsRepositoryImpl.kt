package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
    }

    override suspend fun getUserStatistics(userId: String): Result<UserStatistics?> = execute {
        userStatisticsDao.getById(userId)?.toDomainModel()
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
        val language = transcriptionHistoryDao.getMostUsedLanguage()
        val model = transcriptionHistoryDao.getMostUsedModel()
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