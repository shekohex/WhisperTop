package me.shadykhalifa.whispertop.initialization

import android.content.Context
import android.util.Log
import me.shadykhalifa.whispertop.workers.DailyStatsAggregatorWorker

object WorkManagerInitializer {
    
    private const val TAG = "WorkManagerInitializer"
    
    fun initialize(context: Context) {
        try {
            // Schedule periodic daily statistics aggregation
            DailyStatsAggregatorWorker.schedulePeriodicAggregation(context)
            
            Log.d(TAG, "WorkManager background tasks scheduled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule WorkManager background tasks", e)
        }
    }
    
    fun cleanup(context: Context) {
        try {
            // Cancel all scheduled work
            DailyStatsAggregatorWorker.cancelScheduledAggregation(context)
            
            Log.d(TAG, "WorkManager background tasks cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel WorkManager background tasks", e)
        }
    }
}