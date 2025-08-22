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
    
    // Flow-based queries for efficient filtering (not paged)
    @Query("""
        SELECT * FROM transcription_history 
        WHERE text LIKE '%' || :query || '%' 
        ORDER BY timestamp DESC
        LIMIT 1000
    """)
    fun searchByTextFlow(query: String): Flow<List<TranscriptionHistoryEntity>>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
        LIMIT 1000
    """)
    fun getByDateRangeFlow(startTime: Long, endTime: Long): Flow<List<TranscriptionHistoryEntity>>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE text LIKE '%' || :query || '%' 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp DESC
        LIMIT 1000
    """)
    fun searchByTextAndDateRangeFlow(
        query: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryEntity>>
    
    // Enhanced paging queries with sorting
    @Query("""
        SELECT * FROM transcription_history 
        ORDER BY 
        CASE WHEN :sortBy = 'timestamp_desc' THEN timestamp END DESC,
        CASE WHEN :sortBy = 'timestamp_asc' THEN timestamp END ASC,
        CASE WHEN :sortBy = 'duration_desc' THEN duration END DESC,
        CASE WHEN :sortBy = 'duration_asc' THEN duration END ASC,
        CASE WHEN :sortBy = 'wordCount_desc' THEN wordCount END DESC,
        CASE WHEN :sortBy = 'wordCount_asc' THEN wordCount END ASC,
        CASE WHEN :sortBy = 'confidence_desc' THEN confidence END DESC,
        CASE WHEN :sortBy = 'confidence_asc' THEN confidence END ASC
    """)
    fun getAllPagedWithSort(sortBy: String): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE text LIKE '%' || :query || '%' 
        ORDER BY 
        CASE WHEN :sortBy = 'timestamp_desc' THEN timestamp END DESC,
        CASE WHEN :sortBy = 'timestamp_asc' THEN timestamp END ASC,
        CASE WHEN :sortBy = 'duration_desc' THEN duration END DESC,
        CASE WHEN :sortBy = 'duration_asc' THEN duration END ASC,
        CASE WHEN :sortBy = 'wordCount_desc' THEN wordCount END DESC,
        CASE WHEN :sortBy = 'wordCount_asc' THEN wordCount END ASC,
        CASE WHEN :sortBy = 'confidence_desc' THEN confidence END DESC,
        CASE WHEN :sortBy = 'confidence_asc' THEN confidence END ASC
    """)
    fun searchByTextWithSort(query: String, sortBy: String): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE (:startTime IS NULL OR timestamp >= :startTime) 
        AND (:endTime IS NULL OR timestamp <= :endTime)
        ORDER BY 
        CASE WHEN :sortBy = 'timestamp_desc' THEN timestamp END DESC,
        CASE WHEN :sortBy = 'timestamp_asc' THEN timestamp END ASC,
        CASE WHEN :sortBy = 'duration_desc' THEN duration END DESC,
        CASE WHEN :sortBy = 'duration_asc' THEN duration END ASC,
        CASE WHEN :sortBy = 'wordCount_desc' THEN wordCount END DESC,
        CASE WHEN :sortBy = 'wordCount_asc' THEN wordCount END ASC,
        CASE WHEN :sortBy = 'confidence_desc' THEN confidence END DESC,
        CASE WHEN :sortBy = 'confidence_asc' THEN confidence END ASC
    """)
    fun getByDateRangeWithSort(
        startTime: Long?, 
        endTime: Long?, 
        sortBy: String
    ): PagingSource<Int, TranscriptionHistoryEntity>
    
    @Query("""
        SELECT * FROM transcription_history 
        WHERE (:query = '' OR text LIKE '%' || :query || '%') 
        AND (:startTime IS NULL OR timestamp >= :startTime) 
        AND (:endTime IS NULL OR timestamp <= :endTime)
        ORDER BY 
        CASE WHEN :sortBy = 'timestamp_desc' THEN timestamp END DESC,
        CASE WHEN :sortBy = 'timestamp_asc' THEN timestamp END ASC,
        CASE WHEN :sortBy = 'duration_desc' THEN duration END DESC,
        CASE WHEN :sortBy = 'duration_asc' THEN duration END ASC,
        CASE WHEN :sortBy = 'wordCount_desc' THEN wordCount END DESC,
        CASE WHEN :sortBy = 'wordCount_asc' THEN wordCount END ASC,
        CASE WHEN :sortBy = 'confidence_desc' THEN confidence END DESC,
        CASE WHEN :sortBy = 'confidence_asc' THEN confidence END ASC
    """)
    fun searchWithFiltersAndSort(
        query: String,
        startTime: Long?,
        endTime: Long?,
        sortBy: String
    ): PagingSource<Int, TranscriptionHistoryEntity>
    
    // Bulk deletion
    @Query("DELETE FROM transcription_history WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int
    
    // Export queries - chunked for memory efficiency
    @Query("""
        SELECT * FROM transcription_history 
        WHERE (:startTime IS NULL OR timestamp >= :startTime) 
        AND (:endTime IS NULL OR timestamp <= :endTime)
        ORDER BY timestamp DESC
        LIMIT :limit OFFSET :offset
    """)
    suspend fun getForExportChunk(
        startTime: Long?, 
        endTime: Long?, 
        limit: Int, 
        offset: Int
    ): List<TranscriptionHistoryEntity>
    
    @Query("""
        SELECT COUNT(*) FROM transcription_history 
        WHERE (:startTime IS NULL OR timestamp >= :startTime) 
        AND (:endTime IS NULL OR timestamp <= :endTime)
    """)
    suspend fun getExportCount(startTime: Long?, endTime: Long?): Long
    
    @Query("DELETE FROM transcription_history WHERE timestamp < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
}