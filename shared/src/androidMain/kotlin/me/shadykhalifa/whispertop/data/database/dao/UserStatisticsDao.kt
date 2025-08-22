package me.shadykhalifa.whispertop.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
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
}