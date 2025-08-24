package me.shadykhalifa.whispertop.domain.repositories

import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.utils.Result

/**
 * Repository interface for transcription database operations focused on export and retention
 */
interface TranscriptionDatabaseRepository {
    
    /**
     * Get all transcriptions for export within date range and optional retention policy
     */
    suspend fun getAllForExport(
        dateRange: DateRange,
        retentionPolicyId: String? = null,
        limit: Int = 1000,
        offset: Int = 0
    ): Result<List<TranscriptionHistoryItem>>
    
    /**
     * Mark multiple transcriptions as exported
     */
    suspend fun markAsExported(ids: List<String>): Result<Int>
    
    /**
     * Get transcriptions by retention policy
     */
    suspend fun getByRetentionPolicy(policyId: String): Result<List<TranscriptionHistoryItem>>
    
    /**
     * Get expired transcriptions that can be deleted based on retention policy
     */
    suspend fun getExpiredByRetentionPolicy(policyId: String, cutoffTime: Long): Result<List<TranscriptionHistoryItem>>
    
    /**
     * Bulk delete transcriptions by IDs
     */
    suspend fun bulkDelete(ids: List<String>): Result<Int>
    
    /**
     * Set protection status for multiple transcriptions
     */
    suspend fun setProtectionStatus(ids: List<String>, isProtected: Boolean): Result<Int>
    
    /**
     * Get count of transcriptions by retention policy
     */
    suspend fun getCountByRetentionPolicy(policyId: String): Result<Long>
    
    /**
     * Delete expired transcriptions by retention policy
     */
    suspend fun deleteExpiredByRetentionPolicy(policyId: String, cutoffTime: Long): Result<Int>
    
    /**
     * Get total count for export within date range
     */
    suspend fun getExportCount(dateRange: DateRange, retentionPolicyId: String? = null): Result<Long>
    
    /**
     * Update retention policy for all transcriptions or specific ones
     */
    suspend fun updateRetentionPolicy(policy: me.shadykhalifa.whispertop.domain.models.RetentionPolicy): Result<Int>
    
    /**
     * Get data summary for export/privacy dashboard
     */
    suspend fun getDataSummary(): Result<me.shadykhalifa.whispertop.domain.models.DataSummary>
}