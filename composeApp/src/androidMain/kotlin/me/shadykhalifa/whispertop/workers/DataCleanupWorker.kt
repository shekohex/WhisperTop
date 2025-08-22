package me.shadykhalifa.whispertop.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.BackoffPolicy
import androidx.work.WorkManager
import android.util.Log
import me.shadykhalifa.whispertop.domain.services.DataRetentionService
import me.shadykhalifa.whispertop.utils.Result
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class DataCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {
    
    companion object {
        private const val TAG = "DataCleanupWorker"
        const val WORK_NAME = "data_cleanup_work"
        
        // Input/Output keys
        const val KEY_CLEANUP_TYPE = "cleanup_type"
        const val KEY_SESSIONS_DELETED = "sessions_deleted"
        const val KEY_TRANSCRIPTIONS_DELETED = "transcriptions_deleted"
        const val KEY_BYTES_FREED = "bytes_freed"
        
        enum class CleanupType {
            NORMAL, EMERGENCY, TRANSCRIPTIONS_ONLY
        }
        
        fun schedulePeriodicCleanup(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
                
            val cleanupRequest = PeriodicWorkRequestBuilder<DataCleanupWorker>(
                1, TimeUnit.DAYS,  // Run daily
                6, TimeUnit.HOURS  // Flex interval
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    15, TimeUnit.MINUTES
                )
                .build()
                
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                    cleanupRequest
                )
        }
        
        fun scheduleImmediateCleanup(context: Context, cleanupType: CleanupType = CleanupType.NORMAL) {
            val immediateRequest = OneTimeWorkRequestBuilder<DataCleanupWorker>()
                .setInputData(
                    androidx.work.workDataOf(
                        KEY_CLEANUP_TYPE to cleanupType.name
                    )
                )
                .build()
                
            WorkManager.getInstance(context).enqueue(immediateRequest)
        }
        
        fun cancelScheduledCleanup(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
    
    private val dataRetentionService: DataRetentionService by inject()
    
    override suspend fun doWork(): Result<androidx.work.ListenableWorker.Result> {
        return try {
            Log.d(TAG, "Starting data cleanup work")
            
            val cleanupType = inputData.getString(KEY_CLEANUP_TYPE)?.let { 
                CleanupType.valueOf(it) 
            } ?: CleanupType.NORMAL
            
            val cleanupResult = when (cleanupType) {
                CleanupType.NORMAL -> dataRetentionService.cleanupOldData()
                CleanupType.EMERGENCY -> dataRetentionService.enforceDataRetentionPolicies()
                CleanupType.TRANSCRIPTIONS_ONLY -> {
                    val transcriptionsResult = dataRetentionService.cleanupTranscriptionsOnly()
                    when (transcriptionsResult) {
                        is me.shadykhalifa.whispertop.utils.Result.Success -> {
                            me.shadykhalifa.whispertop.utils.Result.Success(
                                me.shadykhalifa.whispertop.domain.services.DataCleanupReport(
                                    sessionsDeleted = 0,
                                    transcriptionsDeleted = transcriptionsResult.data,
                                    transcriptionHistoryDeleted = 0,
                                    bytesFreed = transcriptionsResult.data * 500L,
                                    oldestRemainingSession = null,
                                    cleanupDurationMs = 0
                                )
                            )
                        }
                        is me.shadykhalifa.whispertop.utils.Result.Error -> transcriptionsResult
                        else -> me.shadykhalifa.whispertop.utils.Result.Error(RuntimeException("Unexpected result type"))
                    }
                }
            }
            
            when (cleanupResult) {
                is me.shadykhalifa.whispertop.utils.Result.Success -> {
                    val report = cleanupResult.data
                    Log.i(TAG, "Data cleanup completed successfully: " +
                          "sessions=${report.sessionsDeleted}, " +
                          "transcriptions=${report.transcriptionsDeleted}, " +
                          "bytesFreed=${report.bytesFreed}")
                    
                    val outputData = androidx.work.workDataOf(
                        KEY_SESSIONS_DELETED to report.sessionsDeleted,
                        KEY_TRANSCRIPTIONS_DELETED to report.transcriptionsDeleted,
                        KEY_BYTES_FREED to report.bytesFreed
                    )
                    
                    Result.Success(androidx.work.ListenableWorker.Result.success(outputData))
                }
                is me.shadykhalifa.whispertop.utils.Result.Error -> {
                    Log.e(TAG, "Data cleanup failed: ${cleanupResult.exception.message}", cleanupResult.exception)
                    Result.Success(androidx.work.ListenableWorker.Result.failure())
                }
                else -> {
                    Log.w(TAG, "Unexpected cleanup result type")
                    Result.Success(androidx.work.ListenableWorker.Result.failure())
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during data cleanup", e)
            Result.Success(androidx.work.ListenableWorker.Result.failure())
        }
    }
}