package me.shadykhalifa.whispertop.data.database.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity

@Dao
interface TranscriptionHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transcription: TranscriptionHistoryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transcriptions: List<TranscriptionHistoryEntity>)
    
    @Update
    suspend fun update(transcription: TranscriptionHistoryEntity)
    
    @Delete
    suspend fun delete(transcription: TranscriptionHistoryEntity)
    
    @Query("DELETE FROM transcription_history WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("DELETE FROM transcription_history")
    suspend fun deleteAll()
    
    @Query("SELECT * FROM transcription_history WHERE id = :id")
    suspend fun getById(id: String): TranscriptionHistoryEntity?
    
    @Query("SELECT * FROM transcription_history ORDER BY timestamp DESC")
    fun getAllPaged(): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("SELECT * FROM transcription_history ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<TranscriptionHistoryEntity>>
    
    @Query("SELECT * FROM transcription_history ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<TranscriptionHistoryEntity>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE text LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
    """)
    fun searchByText(query: String): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    fun getByDateRange(startTime: Long, endTime: Long): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE text LIKE '%' || :query || '%' 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
    """)
    fun searchByTextAndDateRange(
        query: String,
        startTime: Long,
        endTime: Long
    ): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("SELECT COUNT(*) FROM transcription_history")
    suspend fun getCount(): Long
    
    @Query("SELECT SUM(duration) FROM transcription_history WHERE duration IS NOT NULL")
    suspend fun getTotalDuration(): Float?
    
    @Query("SELECT AVG(confidence) FROM transcription_history WHERE confidence IS NOT NULL")
    suspend fun getAverageConfidence(): Float?
    
    @Query("""
        SELECT language, COUNT(*) as count 
        FROM transcription_history 
        WHERE language IS NOT NULL 
        GROUP BY language 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getMostUsedLanguage(): String?
    
    @Query("""
        SELECT model, COUNT(*) as count 
        FROM transcription_history 
        WHERE model IS NOT NULL 
        GROUP BY model 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getMostUsedModel(): String?
    
    @Query("SELECT COUNT(*) FROM transcription_history WHERE timestamp >= :startOfDay")
    suspend fun getDailyTranscriptionCount(startOfDay: Long): Long
}