package me.shadykhalifa.whispertop.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity

@Dao
interface UserStatisticsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(statistics: UserStatisticsEntity)
    
    @Update
    suspend fun update(statistics: UserStatisticsEntity)
    
    @Query("SELECT * FROM user_statistics WHERE id = :id")
    suspend fun getById(id: String): UserStatisticsEntity?
    
    @Query("SELECT * FROM user_statistics WHERE id = :id")
    fun getByIdFlow(id: String): Flow<UserStatisticsEntity?>
    
    @Query("SELECT * FROM user_statistics ORDER BY updatedAt DESC")
    suspend fun getAll(): List<UserStatisticsEntity>
    
    @Query("SELECT * FROM user_statistics ORDER BY updatedAt DESC")
    fun getAllFlow(): Flow<List<UserStatisticsEntity>>
    
    @Query("DELETE FROM user_statistics WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM user_statistics")
    suspend fun deleteAll()
    
    @Query("""
        UPDATE user_statistics 
        SET totalTranscriptions = totalTranscriptions + 1,
            totalDuration = totalDuration + :duration,
            dailyUsageCount = :dailyUsageCount,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun incrementTranscriptionStats(
        id: String,
        duration: Float,
        dailyUsageCount: Long,
        updatedAt: Long
    )
    
    @Query("""
        UPDATE user_statistics 
        SET averageAccuracy = :accuracy,
            mostUsedLanguage = :language,
            mostUsedModel = :model,
            updatedAt = :updatedAt
        WHERE id = :id
    """)
    suspend fun updateDerivedStats(
        id: String,
        accuracy: Float?,
        language: String?,
        model: String?,
        updatedAt: Long
    )
    
    @Query("""
        UPDATE user_statistics
        SET totalSessions = totalSessions + :totalSessions,
            totalWords = totalWords + :totalWords,
            totalSpeakingTimeMs = totalSpeakingTimeMs + :totalSpeakingTime,
            totalTranscriptions = totalTranscriptions + :totalSessions,
            averageWordsPerMinute = CASE
                WHEN totalSpeakingTimeMs + :totalSpeakingTime > 0
                THEN (totalWords + :totalWords) / ((totalSpeakingTimeMs + :totalSpeakingTime) / 60000.0)
                ELSE averageWordsPerMinute
            END,
            averageWordsPerSession = CASE
                WHEN totalSessions + :totalSessions > 0
                THEN (totalWords + :totalWords) / (totalSessions + :totalSessions)
                ELSE averageWordsPerSession
            END,
            updatedAt = :currentTime
        WHERE id = :userId
    """)
    suspend fun updateDailyAggregatedStats(
        userId: String,
        totalSessions: Int,
        totalWords: Long,
        totalSpeakingTime: Long,
        currentTime: Long
    )

    @Transaction
    suspend fun insertOrUpdate(statistics: UserStatisticsEntity) {
        val existing = getById(statistics.id)
        if (existing != null) {
            update(statistics.copy(createdAt = existing.createdAt))
        } else {
            insert(statistics)
        }
    }
}