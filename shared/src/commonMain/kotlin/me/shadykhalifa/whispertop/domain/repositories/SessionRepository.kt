package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Instant
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.utils.Result

interface SessionRepository {
    
    suspend fun createSession(session: TranscriptionSession): Result<Unit>
    
    suspend fun getSession(sessionId: String): Result<TranscriptionSession?>
    
    suspend fun getAllSessions(userId: String): Result<List<TranscriptionSession>>
    
    fun getAllSessionsFlow(userId: String): Flow<List<TranscriptionSession>>
    
    suspend fun getSessionsInRange(
        userId: String,
        startTime: Instant,
        endTime: Instant
    ): Result<List<TranscriptionSession>>
    
    suspend fun getSessionsByDateRange(
        userId: String,
        startTime: Instant,
        endTime: Instant,
        limit: Int? = null
    ): Result<List<TranscriptionSession>>
    
    suspend fun updateSession(session: TranscriptionSession): Result<Unit>
    
    suspend fun deleteSession(sessionId: String): Result<Unit>
    
    suspend fun deleteAllSessions(userId: String): Result<Unit>
    
    suspend fun getSessionCount(userId: String): Result<Int>
    
    suspend fun getTotalWordsTranscribed(userId: String): Result<Long>
    
    suspend fun getTotalSpeakingTime(userId: String): Result<Long>
}