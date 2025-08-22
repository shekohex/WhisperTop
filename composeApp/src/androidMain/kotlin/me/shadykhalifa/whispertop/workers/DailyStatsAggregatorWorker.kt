package me.shadykhalifa.whispertop.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.BackoffPolicy
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.workDataOf
import android.util.Log
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import me.shadykhalifa.whispertop.domain.usecases.StatisticsCalculatorUseCase
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.ErrorMonitor
import me.shadykhalifa.whispertop.utils.TimezoneHandler
import me.shadykhalifa.whispertop.utils.TimezoneInfo
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.TimeUnit

class DailyStatsAggregatorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams), KoinComponent {
    
    companion object {
        private const val TAG = "DailyStatsAggregator"
        const val WORK_NAME = "daily_stats_aggregation_work"
        
        // Input/Output keys
        const val KEY_AGGREGATION_DATE = "aggregation_date"
        const val KEY_SESSIONS_PROCESSED = "sessions_processed"
        const val KEY_STATS_UPDATED = "stats_updated"
        const val KEY_ERRORS_COUNT = "errors_count"
        
        fun schedulePeriodicAggregation(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(true)
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()
                
            val aggregationRequest = PeriodicWorkRequestBuilder<DailyStatsAggregatorWorker>(
                24, TimeUnit.HOURS,  // Run daily
                6, TimeUnit.HOURS    // Flex interval - run in last 6 hours of 24h period
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
                    ExistingPeriodicWorkPolicy.KEEP, // Keep existing to avoid duplicates
                    aggregationRequest
                )
            
            Log.i(TAG, "Scheduled periodic daily statistics aggregation")
        }
        
        fun cancelScheduledAggregation(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
            Log.i(TAG, "Cancelled scheduled daily statistics aggregation")
        }
    }
    
    private val statisticsCalculator: StatisticsCalculatorUseCase by inject()
    private val sessionMetricsRepository: SessionMetricsRepository by inject()
    private val userStatisticsRepository: UserStatisticsRepository by inject()
    
    override suspend fun doWork(): androidx.work.ListenableWorker.Result {
        val startTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Starting daily statistics aggregation work")
            ErrorMonitor.reportMetric("worker.daily_stats.started", 1.0, mapOf("worker" to "DailyStatsAggregator"))
            
            val currentTime = Clock.System.now()
            val timeZone = TimeZone.currentSystemDefault()
            val currentDate = currentTime.toLocalDateTime(timeZone).date
            
            // Get the date to aggregate (yesterday to ensure all data is available)
            val aggregationDate = currentDate.minus(DatePeriod(days = 1))
            
            Log.d(TAG, "Aggregating statistics for date: $aggregationDate")
            
            var sessionsProcessed = 0
            var statsUpdated = 0
            var errorsCount = 0
            
            // Calculate daily statistics
            val calculationResult = statisticsCalculator.calculateDailyStatistics(aggregationDate)
            when (calculationResult) {
                is me.shadykhalifa.whispertop.utils.Result.Success -> {
                    val dailyStats = calculationResult.data
                    sessionsProcessed = dailyStats.sessionsCount
                    
                    Log.d(TAG, "Calculated daily stats: $dailyStats")
                    
                    // Update user statistics with aggregated data
                    val updateResult = userStatisticsRepository.updateDailyAggregatedStats(
                        date = aggregationDate,
                        totalSessions = dailyStats.sessionsCount,
                        totalWords = dailyStats.totalWords,
                        totalSpeakingTime = dailyStats.totalSpeakingTimeMs,
                        averageSessionDuration = dailyStats.averageSessionDuration,
                        peakUsageHour = dailyStats.peakUsageHour
                    )
                    
                    when (updateResult) {
                        is me.shadykhalifa.whispertop.utils.Result.Success -> {
                            statsUpdated++
                            Log.d(TAG, "Successfully updated daily stats for $aggregationDate")
                        }
                        is me.shadykhalifa.whispertop.utils.Result.Error -> {
                            errorsCount++
                            Log.e(TAG, "Failed to update daily statistics", updateResult.exception)
                            ErrorMonitor.reportError(
                                component = "DailyStatsAggregator",
                                operation = "updateDailyAggregatedStats",
                                throwable = updateResult.exception,
                                context = mapOf("date" to aggregationDate.toString())
                            )
                        }
                        is me.shadykhalifa.whispertop.utils.Result.Loading -> {
                            // Should not happen in this context
                        }
                    }
                }
                is Result.Error -> {
                    errorsCount++
                    Log.e(TAG, "Failed to calculate daily statistics", calculationResult.exception)
                    ErrorMonitor.reportError(
                        component = "DailyStatsAggregator",
                        operation = "calculateDailyStatistics",
                        throwable = calculationResult.exception,
                        context = mapOf("date" to aggregationDate.toString())
                    )
                }
                is Result.Loading -> {
                    // Should not happen in this context
                    errorsCount++
                }
            }
            
            // Perform data retention cleanup if needed
            try {
                val retentionResult = statisticsCalculator.enforceDataRetentionPolicies()
                when (retentionResult) {
                    is me.shadykhalifa.whispertop.utils.Result.Success -> {
                        Log.d(TAG, "Data retention policies enforced successfully")
                    }
                    is me.shadykhalifa.whispertop.utils.Result.Error -> {
                        errorsCount++
                        Log.w(TAG, "Failed to enforce data retention policies", retentionResult.exception)
                    }
                    is me.shadykhalifa.whispertop.utils.Result.Loading -> {
                        // Should not happen in this context
                    }
                }
            } catch (e: Exception) {
                errorsCount++
                Log.w(TAG, "Error during data retention enforcement", e)
            }
            
            // Handle timezone changes
            try {
                handleTimezoneChanges(timeZone)
            } catch (e: Exception) {
                errorsCount++
                Log.w(TAG, "Error handling timezone changes", e)
            }
            
            val outputData = workDataOf(
                KEY_AGGREGATION_DATE to aggregationDate.toString(),
                KEY_SESSIONS_PROCESSED to sessionsProcessed,
                KEY_STATS_UPDATED to statsUpdated,
                KEY_ERRORS_COUNT to errorsCount
            )
            
            Log.d(TAG, "Daily statistics aggregation completed. Processed: $sessionsProcessed sessions, Updated: $statsUpdated stats, Errors: $errorsCount")
            
            // Report performance metrics
            val duration = System.currentTimeMillis() - startTime
            ErrorMonitor.reportMetric("worker.daily_stats.duration_ms", duration.toDouble())
            ErrorMonitor.reportMetric("worker.daily_stats.sessions_processed", sessionsProcessed.toDouble())
            ErrorMonitor.reportMetric("worker.daily_stats.errors", errorsCount.toDouble())
            
            if (errorsCount > 0 && sessionsProcessed == 0) {
                // Critical failure - no data processed and errors occurred
                ErrorMonitor.reportError(
                    component = "DailyStatsAggregator",
                    operation = "doWork",
                    throwable = RuntimeException("Critical failure: No data processed with $errorsCount errors"),
                    context = mapOf(
                        "date" to aggregationDate.toString(),
                        "duration_ms" to duration.toString()
                    )
                )
                androidx.work.ListenableWorker.Result.retry()
            } else {
                ErrorMonitor.reportMetric("worker.daily_stats.completed", 1.0, mapOf("status" to "success"))
                androidx.work.ListenableWorker.Result.success(outputData)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during daily statistics aggregation", e)
            ErrorMonitor.reportError(
                component = "DailyStatsAggregator",
                operation = "doWork",
                throwable = e,
                context = mapOf(
                    "duration_ms" to (System.currentTimeMillis() - startTime).toString()
                )
            )
            ErrorMonitor.reportMetric("worker.daily_stats.completed", 1.0, mapOf("status" to "failed"))
            androidx.work.ListenableWorker.Result.retry()
        }
    }
    
    private suspend fun handleTimezoneChanges(currentTimeZone: TimeZone) {
        try {
            val currentTimezoneInfo = TimezoneHandler.getCurrentTimezoneInfo()
            Log.d(TAG, "Current timezone: ${currentTimezoneInfo.zoneId}")
            
            // Get last known timezone from shared preferences
            val sharedPrefs = applicationContext.getSharedPreferences("timezone_cache", android.content.Context.MODE_PRIVATE)
            val lastKnownTimezoneJson = sharedPrefs.getString("last_known_timezone", null)
            val lastKnownTimezone = lastKnownTimezoneJson?.let {
                try {
                    kotlinx.serialization.json.Json.decodeFromString<TimezoneInfo>(it)
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse stored timezone info", e)
                    null
                }
            }
            
            // Check if timezone has changed
            if (TimezoneHandler.hasTimezoneChanged(lastKnownTimezone)) {
                Log.i(TAG, "Timezone change detected. Last known: ${lastKnownTimezone?.zoneId ?: "unknown"}")
                
                // Get dates that need re-aggregation
                val datesToReaggregate = TimezoneHandler.getDatesNeedingReaggregation(lastKnownTimezone)
                
                if (datesToReaggregate.isNotEmpty()) {
                    Log.i(TAG, "Re-aggregating statistics for ${datesToReaggregate.size} dates due to timezone change")
                    
                    // Re-aggregate statistics for affected dates
                    datesToReaggregate.forEach { date ->
                        try {
                            val recalculationResult = statisticsCalculator.calculateDailyStatistics(date)
                            when (recalculationResult) {
                                is me.shadykhalifa.whispertop.utils.Result.Success -> {
                                    val dailyStats = recalculationResult.data
                                    userStatisticsRepository.updateDailyAggregatedStats(
                                        date = date,
                                        totalSessions = dailyStats.sessionsCount,
                                        totalWords = dailyStats.totalWords,
                                        totalSpeakingTime = dailyStats.totalSpeakingTimeMs,
                                        averageSessionDuration = dailyStats.averageSessionDuration,
                                        peakUsageHour = dailyStats.peakUsageHour
                                    )
                                    Log.d(TAG, "Re-aggregated statistics for $date")
                                }
                is me.shadykhalifa.whispertop.utils.Result.Error -> {
                                    Log.w(TAG, "Failed to re-aggregate statistics for $date", recalculationResult.exception)
                                }
                is me.shadykhalifa.whispertop.utils.Result.Loading -> {
                                    // Should not happen in this context
                                }
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Error re-aggregating statistics for $date", e)
                        }
                    }
                }
            }
            
            // Store current timezone for future reference
            val currentTimezoneJson = kotlinx.serialization.json.Json.encodeToString(TimezoneInfo.serializer(), currentTimezoneInfo)
            sharedPrefs.edit()
                .putString("last_known_timezone", currentTimezoneJson)
                .apply()
            
            Log.d(TAG, "Timezone handling completed successfully")
            
        } catch (e: Exception) {
            Log.w(TAG, "Error in timezone handling", e)
            throw e
        }
    }
}