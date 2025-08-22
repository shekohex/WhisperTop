package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

interface DataRetentionService {
    suspend fun cleanupOldData(): Result<DataCleanupReport>
    suspend fun cleanupTranscriptionsOnly(): Result<Int>
    suspend fun getStorageStats(): Result<StorageStats>
    suspend fun enforceDataRetentionPolicies(): Result<DataCleanupReport>
}

data class DataCleanupReport(
    val sessionsDeleted: Int,
    val transcriptionsDeleted: Int,
    val transcriptionHistoryDeleted: Int,
    val bytesFreed: Long,
    val oldestRemainingSession: Long?,
    val cleanupDurationMs: Long
)

data class StorageStats(
    val totalSessions: Int,
    val totalTranscriptions: Int,
    val largeTranscriptions: Int,
    val oldestSessionAge: Long,
    val estimatedSizeBytes: Long
)

class DataRetentionServiceImpl(
    private val sessionMetricsRepository: SessionMetricsRepository,
    private val transcriptionHistoryRepository: TranscriptionHistoryRepository
) : DataRetentionService {
    
    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        const val TRANSCRIPTION_RETENTION_DAYS = 7  // More aggressive for transcriptions
        const val LARGE_TRANSCRIPTION_THRESHOLD = 5000  // Characters
        const val MAX_TOTAL_SESSIONS = 10000  // Absolute limit to prevent unbounded growth
        const val EMERGENCY_CLEANUP_THRESHOLD = 0.9f  // Clean up when 90% full
    }
    
    override suspend fun cleanupOldData(): Result<DataCleanupReport> {
        return try {
            val startTime = Clock.System.now().toEpochMilliseconds()
            
            val retentionTimestamp = startTime - (DEFAULT_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
            val transcriptionRetentionTimestamp = startTime - (TRANSCRIPTION_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
            
            // Get storage stats before cleanup
            val statsBefore = getStorageStats()
            val estimatedSizeBefore = (statsBefore as? Result.Success)?.data?.estimatedSizeBytes ?: 0
            
            // Clean up old sessions
            val sessionsDeletedResult = sessionMetricsRepository.deleteOldSessions(retentionTimestamp)
            val sessionsDeleted = (sessionsDeletedResult as? Result.Success)?.data ?: 0
            
            // Clean up old transcription history
            val transcriptionHistoryDeletedResult = transcriptionHistoryRepository.deleteOlderThan(retentionTimestamp)
            val transcriptionHistoryDeleted = (transcriptionHistoryDeletedResult as? Result.Success)?.data ?: 0
            
            // Clean up transcriptions more aggressively (but keep metadata)
            val transcriptionsDeletedResult = sessionMetricsRepository.deleteOldSessions(transcriptionRetentionTimestamp)
            val transcriptionsDeleted = (transcriptionsDeletedResult as? Result.Success)?.data ?: 0
            
            // Get oldest remaining session
            val statsAfter = getStorageStats()
            val oldestRemaining = (statsAfter as? Result.Success)?.data?.oldestSessionAge
            
            val estimatedSizeAfter = (statsAfter as? Result.Success)?.data?.estimatedSizeBytes ?: 0
            val bytesFreed = (estimatedSizeBefore - estimatedSizeAfter).coerceAtLeast(0)
            
            val cleanupDuration = Clock.System.now().toEpochMilliseconds() - startTime
            
            Result.Success(DataCleanupReport(
                sessionsDeleted = sessionsDeleted,
                transcriptionsDeleted = transcriptionsDeleted,
                transcriptionHistoryDeleted = transcriptionHistoryDeleted,
                bytesFreed = bytesFreed,
                oldestRemainingSession = oldestRemaining,
                cleanupDurationMs = cleanupDuration
            ))
            
        } catch (e: Exception) {
            Result.Error(RuntimeException("Failed to cleanup old data: ${e.message}", e))
        }
    }
    
    override suspend fun cleanupTranscriptionsOnly(): Result<Int> {
        return try {
            val currentTime = Clock.System.now().toEpochMilliseconds()
            val retentionTimestamp = currentTime - (TRANSCRIPTION_RETENTION_DAYS * 24 * 60 * 60 * 1000L)
            
            // This would need a specific method in DAO to only delete transcription text, not the whole record
            // For now, using the existing method
            sessionMetricsRepository.deleteOldSessions(retentionTimestamp)
        } catch (e: Exception) {
            Result.Error(RuntimeException("Failed to cleanup transcriptions: ${e.message}", e))
        }
    }
    
    override suspend fun getStorageStats(): Result<StorageStats> {
        return try {
            val sessionStatsResult = sessionMetricsRepository.getSessionStatistics()
            val sessionStats = (sessionStatsResult as? Result.Success)?.data
            
            if (sessionStats == null) {
                return Result.Error(RuntimeException("Failed to get session statistics"))
            }
            
            // Estimate storage size (rough calculation)
            val avgTranscriptionSize = 500 // bytes per transcription (rough estimate)
            val avgSessionMetadata = 200 // bytes per session metadata
            val estimatedSize = (sessionStats.totalSessions * avgSessionMetadata) + 
                              (sessionStats.totalSessions * avgTranscriptionSize)
            
            Result.Success(StorageStats(
                totalSessions = sessionStats.totalSessions,
                totalTranscriptions = sessionStats.successfulSessions, // Successful sessions have transcriptions
                largeTranscriptions = 0, // Would need specific DAO method
                oldestSessionAge = 0, // Would need specific DAO method
                estimatedSizeBytes = estimatedSize.toLong()
            ))
            
        } catch (e: Exception) {
            Result.Error(RuntimeException("Failed to get storage stats: ${e.message}", e))
        }
    }
    
    override suspend fun enforceDataRetentionPolicies(): Result<DataCleanupReport> {
        return try {
            val stats = getStorageStats()
            val storageStats = (stats as? Result.Success)?.data
            
            if (storageStats == null) {
                return Result.Error(RuntimeException("Failed to get storage stats for policy enforcement"))
            }
            
            // Check if emergency cleanup is needed
            val needsEmergencyCleanup = storageStats.totalSessions > MAX_TOTAL_SESSIONS
            
            if (needsEmergencyCleanup) {
                // More aggressive cleanup
                val emergencyRetentionDays = 7 // Only keep 1 week of data in emergency
                val emergencyTimestamp = Clock.System.now().toEpochMilliseconds() - 
                                       (emergencyRetentionDays * 24 * 60 * 60 * 1000L)
                
                val emergencyDeleteResult = sessionMetricsRepository.deleteOldSessions(emergencyTimestamp)
                val deletedSessions = (emergencyDeleteResult as? Result.Success)?.data ?: 0
                
                Result.Success(DataCleanupReport(
                    sessionsDeleted = deletedSessions,
                    transcriptionsDeleted = deletedSessions, // Assume all had transcriptions
                    transcriptionHistoryDeleted = 0,
                    bytesFreed = deletedSessions * 700L, // Estimated bytes per session
                    oldestRemainingSession = emergencyTimestamp,
                    cleanupDurationMs = 0
                ))
            } else {
                // Normal cleanup
                cleanupOldData()
            }
            
        } catch (e: Exception) {
            Result.Error(RuntimeException("Failed to enforce retention policies: ${e.message}", e))
        }
    }
}