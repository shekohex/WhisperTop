package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.utils.Result

/**
 * Service for exporting transcription data in various formats
 */
interface ExportService {
    
    /**
     * Export transcriptions with streaming progress updates
     */
    suspend fun exportTranscriptions(
        format: ExportFormat,
        dateRange: DateRange,
        retentionPolicyId: String? = null,
        progressCallback: ((Int) -> Unit)? = null
    ): Flow<ExportResult>
    
    /**
     * Export data with streaming progress updates - alias for exportTranscriptions
     */
    suspend fun exportData(
        format: ExportFormat,
        dateRange: DateRange,
        includeProtectedData: Boolean = false
    ): Flow<ExportResult> = exportTranscriptions(format, dateRange)
    
    /**
     * Export specific transcriptions by IDs
     */
    suspend fun exportTranscriptionsByIds(
        ids: List<String>,
        format: ExportFormat,
        progressCallback: ((Int) -> Unit)? = null
    ): Flow<ExportResult>
    
    /**
     * Create secure file URI for sharing exported data
     */
    suspend fun createSecureFileUri(fileName: String): Result<String>
    
    /**
     * Clean up temporary export files
     */
    suspend fun cleanupTemporaryFiles(): Result<Int>
    
    /**
     * Get export statistics
     */
    suspend fun getExportStatistics(): Result<ExportStatistics>
}

/**
 * Statistics about export operations
 */
data class ExportStatistics(
    val totalExports: Long,
    val totalItemsExported: Long,
    val lastExportTime: Long?,
    val averageExportSize: Long,
    val popularFormats: Map<ExportFormat, Long>
)