package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.SessionMetrics
import me.shadykhalifa.whispertop.utils.Result

interface SessionMetricsRepository {
    suspend fun createSessionMetrics(sessionMetrics: SessionMetrics): Result<Unit>
    suspend fun getSessionMetrics(sessionId: String): Result<SessionMetrics?>
    fun getSessionMetricsFlow(sessionId: String): Flow<SessionMetrics?>
    suspend fun updateSessionMetrics(sessionMetrics: SessionMetrics): Result<Unit>
    suspend fun getRecentSessions(limit: Int): Result<List<SessionMetrics>>
    suspend fun getSessionsByDateRange(startTime: Long, endTime: Long): Result<List<SessionMetrics>>
    suspend fun getSessionsByApp(packageName: String): Result<List<SessionMetrics>>
    suspend fun getSessionStatistics(): Result<SessionStatistics>
    suspend fun deleteOldSessions(olderThan: Long): Result<Int>
    suspend fun deleteAllSessions(): Result<Unit>
    suspend fun getSessionsOlderThan(cutoffTime: Long): Result<List<SessionMetrics>>
    suspend fun deleteSessionsOlderThan(cutoffTime: Long): Result<Int>
    suspend fun updateSessionWithTextInsertionData(
        sessionId: String,
        wordCount: Int,
        characterCount: Int,
        speakingRate: Double,
        transcriptionText: String?,
        transcriptionSuccess: Boolean,
        textInsertionSuccess: Boolean,
        targetAppPackage: String?
    ): Result<Unit>
    fun getAllSessionsFlow(): Flow<List<SessionMetrics>>
    fun getSessionsByDateRangeFlow(startTime: Long, endTime: Long): Flow<List<SessionMetrics>>
    suspend fun getSessionsByDateRangePaginated(
        startTime: Long, 
        endTime: Long, 
        limit: Int, 
        offset: Int
    ): Result<List<SessionMetrics>>
    suspend fun getSessionCountByDateRange(startTime: Long, endTime: Long): Result<Int>
}

data class SessionStatistics(
    val totalSessions: Int,
    val successfulSessions: Int,
    val averageWordCount: Double,
    val averageRecordingDuration: Long,
    val averageSpeakingRate: Double,
    val mostUsedApps: Map<String, Int>,
    val errorStatistics: Map<String, Int>
) {
    fun getSuccessRate(): Float = if (totalSessions > 0) (successfulSessions.toFloat() / totalSessions) * 100 else 0f
}