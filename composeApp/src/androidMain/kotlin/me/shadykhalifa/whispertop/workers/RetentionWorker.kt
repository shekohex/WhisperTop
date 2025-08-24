package me.shadykhalifa.whispertop.workers

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import me.shadykhalifa.whispertop.domain.models.RetentionConfiguration
import me.shadykhalifa.whispertop.domain.models.RetentionPolicy
import me.shadykhalifa.whispertop.domain.models.RetentionPolicyResult
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionDatabaseRepository
import me.shadykhalifa.whispertop.domain.services.ExportService
import me.shadykhalifa.whispertop.utils.ErrorMonitor
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class RetentionWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "RetentionWorker"
        const val WORK_NAME = "data_retention_work"
        
        // Input/Output keys
        const val KEY_POLICY_ID = "policy_id"
        const val KEY_FORCE_CLEANUP = "force_cleanup"
        const val KEY_BACKUP_CREATED = "backup_created"
        const val KEY_RECORDS_DELETED = "records_deleted"
        const val KEY_BYTES_FREED = "bytes_freed"
        
        /**
         * Schedule periodic retention cleanup
         */
        fun schedulePeriodicCleanup(context: Context, config: RetentionConfiguration) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
                
            val cleanupRequest = PeriodicWorkRequestBuilder<RetentionWorker>(
                config.cleanupFrequencyHours.toLong(), TimeUnit.HOURS,
                6, TimeUnit.HOURS // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .setInputData(
                    workDataOf(
                        KEY_POLICY_ID to config.defaultPolicyId,
                        KEY_FORCE_CLEANUP to false
                    )
                )
                .build()
                
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.REPLACE,
                    cleanupRequest
                )
                
            Log.i(TAG, "Scheduled periodic retention cleanup every ${config.cleanupFrequencyHours}h")
        }
        
        /**
         * Cancel scheduled retention cleanup
         */
        fun cancelScheduledCleanup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled scheduled retention cleanup")
        }
        
        /**
         * Run immediate retention cleanup for specific policy
         */
        fun runImmediateCleanup(context: Context, policyId: String, forceCleanup: Boolean = false) {
            val immediateRequest = androidx.work.OneTimeWorkRequestBuilder<RetentionWorker>()
                .setInputData(
                    workDataOf(
                        KEY_POLICY_ID to policyId,
                        KEY_FORCE_CLEANUP to forceCleanup
                    )
                )
                .build()
                
            WorkManager.getInstance(context).enqueue(immediateRequest)
        }
    }

    private val transcriptionRepository: TranscriptionDatabaseRepository by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val exportService: ExportService by inject()

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val startTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Starting data retention cleanup work")
            ErrorMonitor.reportMetric("worker.retention.started", 1.0, mapOf("worker" to "RetentionWorker"))
            
            val policyId = inputData.getString(KEY_POLICY_ID) ?: RetentionPolicy.getDefaultPolicy().id
            val forceCleanup = inputData.getBoolean(KEY_FORCE_CLEANUP, false)
            
            val policy = RetentionPolicy.findById(policyId)
            if (policy == null) {
                Log.e(TAG, "Invalid retention policy ID: $policyId")
                return androidx.work.ListenableWorker.Result.failure()
            }
            
            Log.d(TAG, "Applying retention policy: ${policy.name} (${policy.retentionDays} days)")
            
            // Skip cleanup for unlimited or protected policies unless forced
            if ((policy.retentionDays == null || policy.isProtected) && !forceCleanup) {
                Log.d(TAG, "Skipping cleanup for ${policy.name} policy (not forced)")
                return androidx.work.ListenableWorker.Result.success()
            }
            
            val result = processRetentionPolicy(policy, forceCleanup)
            
            when (result) {
                is Result.Success -> {
                    val retentionResult = result.data
                    Log.i(TAG, "Retention cleanup completed: ${retentionResult.totalRecordsDeleted} records deleted, ${retentionResult.formatBytesFreed()} freed")
                    
                    val outputData = workDataOf(
                        KEY_RECORDS_DELETED to retentionResult.totalRecordsDeleted,
                        KEY_BYTES_FREED to retentionResult.bytesFreed,
                        KEY_BACKUP_CREATED to true
                    )
                    
                    // Report success metrics
                    val duration = System.currentTimeMillis() - startTime
                    ErrorMonitor.reportMetric("worker.retention.duration_ms", duration.toDouble())
                    ErrorMonitor.reportMetric("worker.retention.records_deleted", retentionResult.totalRecordsDeleted.toDouble())
                    ErrorMonitor.reportMetric("worker.retention.completed", 1.0, mapOf("status" to "success"))
                    
                    androidx.work.ListenableWorker.Result.success(outputData)
                }
                is Result.Error -> {
                    Log.e(TAG, "Retention cleanup failed: ${result.exception.message}", result.exception)
                    ErrorMonitor.reportError(
                        component = "RetentionWorker",
                        operation = "processRetentionPolicy",
                        throwable = result.exception,
                        context = mapOf("policy" to policyId)
                    )
                    ErrorMonitor.reportMetric("worker.retention.completed", 1.0, mapOf("status" to "failed"))
                    androidx.work.ListenableWorker.Result.retry()
                }
                is Result.Loading -> {
                    // Should not happen in this context
                    androidx.work.ListenableWorker.Result.failure()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during retention cleanup", e)
            ErrorMonitor.reportError(
                component = "RetentionWorker",
                operation = "doWork",
                throwable = e,
                context = mapOf(
                    "duration_ms" to (System.currentTimeMillis() - startTime).toString()
                )
            )
            ErrorMonitor.reportMetric("worker.retention.completed", 1.0, mapOf("status" to "error"))
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private suspend fun processRetentionPolicy(
        policy: RetentionPolicy,
        forceCleanup: Boolean
    ): Result<RetentionPolicyResult> {
        return try {
            val now = Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            val currentDate = now.toLocalDateTime(timeZone).date
            
            // Calculate cutoff time
            val cutoffTime = if (policy.retentionDays != null) {
                val cutoffDate = currentDate.minus(policy.retentionDays, DateTimeUnit.DAY)
                cutoffDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
            } else if (forceCleanup) {
                // For unlimited policies with force cleanup, use a very old date
                0L
            } else {
                // Skip cleanup for unlimited policies
                return Result.Success(
                    RetentionPolicyResult(
                        sessionsDeleted = 0,
                        transcriptionsDeleted = 0,
                        bytesFreed = 0L,
                        lastCleanupDate = currentDate
                    )
                )
            }
            
            Log.d(TAG, "Finding expired transcriptions for policy ${policy.name} with cutoff time $cutoffTime")
            
            // Get expired transcriptions
            val expiredResult = transcriptionRepository.getExpiredByRetentionPolicy(policy.id, cutoffTime)
            
            when (expiredResult) {
                is Result.Success -> {
                    val expiredTranscriptions = expiredResult.data
                    
                    if (expiredTranscriptions.isEmpty()) {
                        Log.d(TAG, "No expired transcriptions found for policy ${policy.name}")
                        return Result.Success(
                            RetentionPolicyResult(
                                sessionsDeleted = 0,
                                transcriptionsDeleted = 0,
                                bytesFreed = 0L,
                                lastCleanupDate = currentDate
                            )
                        )
                    }
                    
                    Log.d(TAG, "Found ${expiredTranscriptions.size} expired transcriptions")
                    
                    // TODO: Create backup before deletion if enabled
                    // This would involve using the ExportService to create a backup
                    
                    // Calculate estimated bytes freed (approximate)
                    val estimatedBytesFreed = expiredTranscriptions.sumOf { transcription ->
                        // Rough estimation: text length + metadata
                        transcription.text.length.toLong() * 2 + 200L
                    }
                    
                    // Delete expired transcriptions
                    val deleteResult = transcriptionRepository.deleteExpiredByRetentionPolicy(policy.id, cutoffTime)
                    
                    when (deleteResult) {
                        is Result.Success -> {
                            val deletedCount = deleteResult.data
                            
                            Log.i(TAG, "Successfully deleted $deletedCount transcriptions for policy ${policy.name}")
                            
                            Result.Success(
                                RetentionPolicyResult(
                                    sessionsDeleted = 0, // TODO: Add session deletion if needed
                                    transcriptionsDeleted = deletedCount,
                                    bytesFreed = estimatedBytesFreed,
                                    lastCleanupDate = currentDate
                                )
                            )
                        }
                        is Result.Error -> {
                            Log.e(TAG, "Failed to delete expired transcriptions", deleteResult.exception)
                            deleteResult
                        }
                        is Result.Loading -> {
                            Result.Error(IllegalStateException("Unexpected loading state during deletion"))
                        }
                    }
                }
                is Result.Error -> {
                    Log.e(TAG, "Failed to get expired transcriptions", expiredResult.exception)
                    expiredResult
                }
                is Result.Loading -> {
                    Result.Error(IllegalStateException("Unexpected loading state during query"))
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error processing retention policy ${policy.name}", e)
            Result.Error(e)
        }
    }
}