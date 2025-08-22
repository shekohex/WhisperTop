package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.models.TranscriptionStatistics
import me.shadykhalifa.whispertop.utils.Result

interface TranscriptionHistoryRepository {
    
    suspend fun saveTranscription(
        text: String,
        duration: Float?,
        audioFilePath: String?,
        confidence: Float?,
        customPrompt: String?,
        temperature: Float?,
        language: String?,
        model: String?
    ): Result<String>
    
    suspend fun getTranscription(id: String): Result<TranscriptionHistoryItem?>
    
    suspend fun updateTranscription(transcription: TranscriptionHistoryItem): Result<Unit>
    
    suspend fun deleteTranscription(id: String): Result<Unit>
    
    suspend fun deleteAllTranscriptions(): Result<Unit>
    
    fun getAllTranscriptions(): Flow<List<TranscriptionHistoryItem>>
    
    fun getAllTranscriptionsFlow(): Flow<List<TranscriptionHistoryItem>>
    
    suspend fun getRecentTranscriptions(limit: Int): Result<List<TranscriptionHistoryItem>>
    
    fun searchTranscriptions(query: String): Flow<List<TranscriptionHistoryItem>>
    
    fun getTranscriptionsByDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryItem>>
    
    fun searchTranscriptionsByTextAndDateRange(
        query: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryItem>>
    
    suspend fun getTranscriptionStatistics(): Result<TranscriptionStatistics>
}