package me.shadykhalifa.whispertop.data.database.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.data.database.entities.SessionMetricsEntity

data class DailyAggregatedStats(
    val totalSessions: Int,
    val successfulSessions: Int,
    val totalWords: Long,
    val totalCharacters: Long,
    val totalSpeakingTime: Long,
    val averageRecordingDuration: Double?,
    val averageSpeakingRate: Double?
)

data class HourlyUsage(
    val hour: Int,
    val sessionCount: Int
)

data class AppUsage(
    val targetAppPackage: String,
    val count: Int
)

data class ErrorBreakdown(
    val errorType: String,
    val count: Int
)

@Dao
interface SessionMetricsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sessionMetrics: SessionMetricsEntity)
    
    @Update
    suspend fun update(sessionMetrics: SessionMetricsEntity)
    
    @Delete
    suspend fun delete(sessionMetrics: SessionMetricsEntity)
    
    @Query("SELECT * FROM session_metrics WHERE sessionId = :sessionId")
    suspend fun getById(sessionId: String): SessionMetricsEntity?
    
    @Query("SELECT * FROM session_metrics WHERE sessionId = :sessionId")
    fun getByIdFlow(sessionId: String): Flow<SessionMetricsEntity?>
    
    @Query("SELECT * FROM session_metrics ORDER BY sessionStartTime DESC")
    fun getAllFlow(): Flow<List<SessionMetricsEntity>>
    
    @Query("SELECT * FROM session_metrics ORDER BY sessionStartTime DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<SessionMetricsEntity>
    
    @Query("SELECT * FROM session_metrics WHERE sessionStartTime >= :startTime AND sessionStartTime <= :endTime ORDER BY sessionStartTime DESC")
    suspend fun getByDateRange(startTime: Long, endTime: Long): List<SessionMetricsEntity>
    
    @Query("SELECT * FROM session_metrics WHERE sessionStartTime >= :startTime AND sessionStartTime <= :endTime ORDER BY sessionStartTime DESC LIMIT :limit OFFSET :offset")
    suspend fun getByDateRangePaginated(startTime: Long, endTime: Long, limit: Int, offset: Int): List<SessionMetricsEntity>
    
    @Query("SELECT * FROM session_metrics WHERE targetAppPackage = :packageName ORDER BY sessionStartTime DESC")
    suspend fun getByTargetApp(packageName: String): List<SessionMetricsEntity>
    
    @Query("SELECT COUNT(*) FROM session_metrics WHERE transcriptionSuccess = 1")
    suspend fun getSuccessfulSessionCount(): Int
    
    @Query("SELECT COUNT(*) FROM session_metrics")
    suspend fun getTotalSessionCount(): Int
    
    @Query("SELECT AVG(wordCount) FROM session_metrics WHERE transcriptionSuccess = 1")
    suspend fun getAverageWordCount(): Double?
    
    @Query("SELECT AVG(audioRecordingDuration) FROM session_metrics WHERE transcriptionSuccess = 1")
    suspend fun getAverageRecordingDuration(): Long?
    
    @Query("SELECT AVG(speakingRate) FROM session_metrics WHERE transcriptionSuccess = 1 AND speakingRate > 0")
    suspend fun getAverageSpeakingRate(): Double?
    
    @Query("SELECT targetAppPackage, COUNT(*) as count FROM session_metrics WHERE targetAppPackage IS NOT NULL GROUP BY targetAppPackage ORDER BY count DESC LIMIT :limit")
    suspend fun getMostUsedApps(limit: Int = 10): List<AppUsage>
    
    @Query("SELECT errorType, COUNT(*) as count FROM session_metrics WHERE errorType IS NOT NULL GROUP BY errorType ORDER BY count DESC")
    suspend fun getErrorStatistics(): List<ErrorBreakdown>
    
    @Query("DELETE FROM session_metrics WHERE sessionStartTime < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM session_metrics")
    suspend fun deleteAll()
    
    @Query("SELECT COUNT(*) FROM session_metrics WHERE sessionStartTime < :timestamp")
    suspend fun countOlderThan(timestamp: Long): Int
    
    @Query("DELETE FROM session_metrics WHERE transcriptionText IS NOT NULL AND sessionStartTime < :timestamp")
    suspend fun deleteTranscriptionsOlderThan(timestamp: Long): Int
    
    @Query("SELECT sessionStartTime FROM session_metrics ORDER BY sessionStartTime ASC LIMIT 1")
    suspend fun getOldestSessionTimestamp(): Long?
    
    @Query("SELECT COUNT(*) FROM session_metrics WHERE LENGTH(transcriptionText) > :sizeThreshold")
    suspend fun countLargeTranscriptions(sizeThreshold: Int): Int
    
    @Query("DELETE FROM session_metrics WHERE LENGTH(transcriptionText) > :sizeThreshold")
    suspend fun deleteLargeTranscriptions(sizeThreshold: Int): Int
    
    @Query("SELECT * FROM session_metrics WHERE sessionStartTime < :cutoffTime ORDER BY sessionStartTime DESC")
    suspend fun getSessionsOlderThan(cutoffTime: Long): List<SessionMetricsEntity>
    
    @Query("DELETE FROM session_metrics WHERE sessionStartTime < :cutoffTime")
    suspend fun deleteSessionsOlderThan(cutoffTime: Long): Int
    
    // Enhanced aggregation queries for statistics calculation - optimized for idx_daily_stats_covering
    @Query("""
        SELECT 
            COUNT(*) as totalSessions,
            SUM(CASE WHEN transcriptionSuccess = 1 THEN 1 ELSE 0 END) as successfulSessions,
            SUM(wordCount) as totalWords,
            SUM(characterCount) as totalCharacters,
            SUM(audioRecordingDuration) as totalSpeakingTime,
            AVG(audioRecordingDuration) as averageRecordingDuration,
            AVG(CASE WHEN speakingRate > 0 THEN speakingRate ELSE NULL END) as averageSpeakingRate
        FROM session_metrics 
        WHERE sessionStartTime >= :startTime AND sessionStartTime < :endTime
    """)
    suspend fun getDailyAggregatedStats(startTime: Long, endTime: Long): DailyAggregatedStats?
    
    @Query("""
        SELECT 
            strftime('%H', datetime(sessionStartTime/1000, 'unixepoch', 'localtime')) as hour,
            COUNT(*) as sessionCount
        FROM session_metrics 
        WHERE sessionStartTime >= :startTime AND sessionStartTime < :endTime
        GROUP BY hour
        ORDER BY sessionCount DESC
        LIMIT 1
    """)
    suspend fun getPeakUsageHour(startTime: Long, endTime: Long): HourlyUsage?
    
    @Query("""
        SELECT targetAppPackage, COUNT(*) as count 
        FROM session_metrics 
        WHERE sessionStartTime >= :startTime AND sessionStartTime < :endTime 
        AND targetAppPackage IS NOT NULL 
        GROUP BY targetAppPackage 
        ORDER BY count DESC 
        LIMIT 1
    """)
    suspend fun getMostUsedAppInPeriod(startTime: Long, endTime: Long): AppUsage?
    
    @Query("""
        SELECT errorType, COUNT(*) as count 
        FROM session_metrics 
        WHERE sessionStartTime >= :startTime AND sessionStartTime < :endTime 
        AND errorType IS NOT NULL 
        GROUP BY errorType 
        ORDER BY count DESC
    """)
    suspend fun getErrorBreakdownInPeriod(startTime: Long, endTime: Long): List<ErrorBreakdown>
    
    @Query("""
        SELECT COUNT(DISTINCT targetAppPackage) 
        FROM session_metrics 
        WHERE sessionStartTime >= :startTime AND sessionStartTime < :endTime 
        AND targetAppPackage IS NOT NULL
    """)
    suspend fun getUniqueAppsUsedInPeriod(startTime: Long, endTime: Long): Int
    
    @Transaction
    suspend fun insertOrUpdate(sessionMetrics: SessionMetricsEntity) {
        val existing = getById(sessionMetrics.sessionId)
        if (existing != null) {
            update(sessionMetrics.copy(
                createdAt = existing.createdAt,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            insert(sessionMetrics)
        }
    }
    
    @Transaction
    suspend fun batchInsert(sessionMetricsList: List<SessionMetricsEntity>) {
        sessionMetricsList.forEach { sessionMetrics ->
            insert(sessionMetrics)
        }
    }
    
    @Transaction
    suspend fun updateWithTranscriptionData(
        sessionId: String,
        wordCount: Int,
        characterCount: Int,
        speakingRate: Double,
        transcriptionText: String?,
        transcriptionSuccess: Boolean,
        textInsertionSuccess: Boolean,
        targetAppPackage: String?
    ) {
        val existing = getById(sessionId)
        if (existing != null) {
            val updatedMetrics = existing.copy(
                wordCount = wordCount,
                characterCount = characterCount,
                speakingRate = speakingRate,
                transcriptionText = transcriptionText,
                transcriptionSuccess = transcriptionSuccess,
                textInsertionSuccess = textInsertionSuccess,
                targetAppPackage = targetAppPackage,
                sessionEndTime = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            update(updatedMetrics)
        }
    }
}