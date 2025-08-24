package me.shadykhalifa.whispertop.domain.repositories

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.SortOption
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.models.TranscriptionStatistics
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.domain.models.DailyUsage
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
    
    // Enhanced paging methods
    fun getTranscriptionsPaged(
        sortOption: SortOption = SortOption.DateNewest
    ): Flow<PagingData<TranscriptionHistory>>
    
    fun searchTranscriptionsPaged(
        query: String,
        sortOption: SortOption = SortOption.DateNewest
    ): Flow<PagingData<TranscriptionHistory>>
    
    fun getTranscriptionsByDateRangePaged(
        dateRange: DateRange,
        sortOption: SortOption = SortOption.DateNewest
    ): Flow<PagingData<TranscriptionHistory>>
    
    fun searchTranscriptionsWithFiltersPaged(
        query: String = "",
        dateRange: DateRange = DateRange.all(),
        sortOption: SortOption = SortOption.DateNewest
    ): Flow<PagingData<TranscriptionHistory>>
    
    // Enhanced deletion methods  
    suspend fun deleteTranscriptions(ids: List<String>): Result<Int>
    
    suspend fun deleteOlderThan(timestamp: Long): Result<Int>
    
    suspend fun getTranscriptionsOlderThan(cutoffTime: Long): Result<List<TranscriptionHistoryItem>>
    
    suspend fun deleteTranscriptionsOlderThan(cutoffTime: Long): Result<Int>
    
    // Export methods
    suspend fun exportTranscriptions(
        format: ExportFormat,
        dateRange: DateRange = DateRange.all()
    ): Flow<ExportResult>
    
    // Dashboard-specific methods for DashboardViewModel
    suspend fun getRecentTranscriptionSessions(limit: Int): Result<List<TranscriptionSession>>
    
    suspend fun getDailyUsage(startDate: LocalDate, endDate: LocalDate): Result<List<DailyUsage>>
    
    // Performance optimization methods
    suspend fun getTranscriptionHistory(
        offset: Int = 0,
        limit: Int = 50
    ): Result<List<TranscriptionHistoryItem>>
    
    suspend fun searchTranscriptionHistory(
        query: String,
        offset: Int = 0,
        limit: Int = 50
    ): Result<List<TranscriptionHistoryItem>>
    
    suspend fun getTotalHistoryCount(): Result<Long>
}