package me.shadykhalifa.whispertop.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import kotlinx.coroutines.delay
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionDatabaseRepository
import me.shadykhalifa.whispertop.utils.ErrorMonitor

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class BulkDeleteWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {

    companion object {
        private const val TAG = "BulkDeleteWorker"
        private const val NOTIFICATION_CHANNEL_ID = "bulk_delete_channel"
        private const val NOTIFICATION_ID = 2001
        
        // Input/Output keys
        const val KEY_TRANSCRIPTION_IDS = "transcription_ids"
        const val KEY_CONFIRMED = "confirmed"
        const val KEY_CREATE_BACKUP = "create_backup"
        const val KEY_DELETED_COUNT = "deleted_count"
        const val KEY_BACKUP_FILE = "backup_file"
        
        /**
         * Schedule bulk deletion with user confirmation
         */
        fun scheduleBulkDelete(
            context: Context,
            transcriptionIds: List<String>,
            confirmed: Boolean = false,
            createBackup: Boolean = true
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
                
            val deleteRequest = OneTimeWorkRequestBuilder<BulkDeleteWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.LINEAR,
                    10, TimeUnit.SECONDS
                )
                .setInputData(
                    workDataOf(
                        KEY_TRANSCRIPTION_IDS to transcriptionIds.toTypedArray(),
                        KEY_CONFIRMED to confirmed,
                        KEY_CREATE_BACKUP to createBackup
                    )
                )
                .build()
                
            WorkManager.getInstance(context).enqueue(deleteRequest)
            Log.i(TAG, "Scheduled bulk deletion for ${transcriptionIds.size} transcriptions")
        }
    }

    private val transcriptionRepository: TranscriptionDatabaseRepository by inject()
    private val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun getForegroundInfo(): ForegroundInfo {
        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Deleting Transcriptions")
            .setContentText("Processing bulk deletion...")
            .setSmallIcon(R.drawable.ic_delete)
            .setOngoing(true)
            .setProgress(100, 0, false)
            .build()
            
        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val startTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Starting bulk delete work")
            ErrorMonitor.reportMetric("worker.bulk_delete.started", 1.0)
            
            setForeground(getForegroundInfo())
            
            val transcriptionIds = inputData.getStringArray(KEY_TRANSCRIPTION_IDS)?.toList() ?: emptyList()
            val confirmed = inputData.getBoolean(KEY_CONFIRMED, false)
            val createBackup = inputData.getBoolean(KEY_CREATE_BACKUP, true)
            
            if (transcriptionIds.isEmpty()) {
                Log.w(TAG, "No transcription IDs provided for bulk deletion")
                return androidx.work.ListenableWorker.Result.failure()
            }
            
            Log.d(TAG, "Processing bulk deletion of ${transcriptionIds.size} transcriptions")
            
            // Validate confirmation
            if (!confirmed) {
                Log.w(TAG, "Bulk deletion not confirmed by user")
                return androidx.work.ListenableWorker.Result.failure()
            }
            
            val result = processBulkDeletion(transcriptionIds, createBackup)
            
            return when (result) {
                is me.shadykhalifa.whispertop.utils.Result.Success -> {
                    val deletedCount = result.data
                    Log.i(TAG, "Bulk deletion completed: $deletedCount transcriptions deleted")
                    
                    showCompletionNotification(deletedCount)
                    
                    val outputData = workDataOf(
                        KEY_DELETED_COUNT to deletedCount
                        // TODO: Add backup file path if backup was created
                    )
                    
                    // Report success metrics
                    val duration = System.currentTimeMillis() - startTime
                    ErrorMonitor.reportMetric("worker.bulk_delete.duration_ms", duration.toDouble())
                    ErrorMonitor.reportMetric("worker.bulk_delete.records_deleted", deletedCount.toDouble())
                    ErrorMonitor.reportMetric("worker.bulk_delete.completed", 1.0, mapOf("status" to "success"))
                    
                    androidx.work.ListenableWorker.Result.success(outputData)
                }
                    is me.shadykhalifa.whispertop.utils.Result.Error -> {
                    Log.e(TAG, "Bulk deletion failed: ${result.exception.message}", result.exception)
                    
                    showErrorNotification(result.exception.message ?: "Unknown error")
                    
                    ErrorMonitor.reportError(
                        component = "BulkDeleteWorker",
                        operation = "processBulkDeletion",
                        throwable = result.exception,
                        context = mapOf("count" to transcriptionIds.size.toString())
                    )
                    ErrorMonitor.reportMetric("worker.bulk_delete.completed", 1.0, mapOf("status" to "failed"))
                    
                    androidx.work.ListenableWorker.Result.retry()
                }
                    is me.shadykhalifa.whispertop.utils.Result.Loading -> {
                    androidx.work.ListenableWorker.Result.failure()
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during bulk deletion", e)
            
            showErrorNotification("Unexpected error occurred")
            
            ErrorMonitor.reportError(
                component = "BulkDeleteWorker",
                operation = "doWork",
                throwable = e,
                context = mapOf(
                    "duration_ms" to (System.currentTimeMillis() - startTime).toString()
                )
            )
            ErrorMonitor.reportMetric("worker.bulk_delete.completed", 1.0, mapOf("status" to "error"))
            
            androidx.work.ListenableWorker.Result.retry()
        }
    }

    private suspend fun processBulkDeletion(
        transcriptionIds: List<String>,
        createBackup: Boolean
    ): me.shadykhalifa.whispertop.utils.Result<Int> {
        return try {
            val totalIds = transcriptionIds.size
            val batchSize = 100 // Process in batches to avoid overwhelming the database
            var deletedCount = 0
            
            // TODO: Create backup if requested
            // This would involve using ExportService to backup the selected transcriptions
            
            // Process deletions in batches
            transcriptionIds.chunked(batchSize).forEachIndexed { batchIndex, batch ->
                updateProgress(batchIndex, transcriptionIds.size / batchSize)
                
                val result = transcriptionRepository.bulkDelete(batch)
                when (result) {
                    is me.shadykhalifa.whispertop.utils.Result.Success -> {
                        val batchDeleted = result.data
                        deletedCount += batchDeleted
                        Log.d(TAG, "Batch ${batchIndex + 1}: deleted $batchDeleted transcriptions")
                    }
                is me.shadykhalifa.whispertop.utils.Result.Error -> {
                        Log.e(TAG, "Failed to delete batch ${batchIndex + 1}", result.exception)
                        return result
                    }
                is me.shadykhalifa.whispertop.utils.Result.Loading -> {
                        return me.shadykhalifa.whispertop.utils.Result.Error(IllegalStateException("Unexpected loading state"))
                    }
                }
                
                // Brief pause between batches to avoid overwhelming the system
                delay(100)
            }
            
            me.shadykhalifa.whispertop.utils.Result.Success(deletedCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during bulk deletion processing", e)
            me.shadykhalifa.whispertop.utils.Result.Error(e)
        }
    }

    private suspend fun updateProgress(currentBatch: Int, totalBatches: Int) {
        val progress = if (totalBatches > 0) {
            ((currentBatch.toFloat() / totalBatches) * 100).toInt()
        } else 100
        
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Deleting Transcriptions")
            .setContentText("Progress: $progress%")
            .setSmallIcon(R.drawable.ic_delete)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(deletedCount: Int) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Deletion Complete")
            .setContentText("Successfully deleted $deletedCount transcriptions")
            .setSmallIcon(R.drawable.ic_check)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(errorMessage: String) {
        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Deletion Failed")
            .setContentText(errorMessage)
            .setSmallIcon(R.drawable.ic_error)
            .setAutoCancel(true)
            .build()
            
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Bulk Operations",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for bulk data operations"
                setShowBadge(false)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
    }
}