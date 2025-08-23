package me.shadykhalifa.whispertop.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.ServiceRecoveryManager
import org.koin.android.ext.android.inject

/**
 * JobService for periodic health monitoring of critical services
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class ServiceHealthCheckJob : JobService() {
    
    private val audioServiceManager: AudioServiceManager by inject()
    private val serviceRecoveryManager: ServiceRecoveryManager by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    
    companion object {
        private const val JOB_ID = 1001
        private const val HEALTH_CHECK_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
        private const val MIN_LATENCY_MS = 60 * 1000L // 1 minute minimum
        private const val MAX_EXECUTION_DELAY_MS = 15 * 60 * 1000L // 15 minutes maximum
        
        /**
         * Schedule periodic health check job
         */
        fun schedulePeriodicHealthCheck(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
            
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            
            // Cancel existing job first
            jobScheduler.cancel(JOB_ID)
            
            val jobInfo = JobInfo.Builder(JOB_ID, ComponentName(context, ServiceHealthCheckJob::class.java))
                .setPersisted(true)
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_NONE)
                .setPeriodic(HEALTH_CHECK_INTERVAL_MS)
                .setBackoffCriteria(30000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        setRequiresBatteryNotLow(false)
                        setRequiresCharging(false)
                        setRequiresDeviceIdle(false)
                        setRequiresStorageNotLow(false)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setImportantWhileForeground(true)
                    }
                }
                .build()
            
            when (val result = jobScheduler.schedule(jobInfo)) {
                JobScheduler.RESULT_SUCCESS -> {
                    // Job scheduled successfully
                }
                JobScheduler.RESULT_FAILURE -> {
                    // Failed to schedule job - handle gracefully
                }
            }
        }
        
        /**
         * Cancel health check job
         */
        fun cancelHealthCheck(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return
            
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(JOB_ID)
        }
    }
    
    override fun onStartJob(params: JobParameters?): Boolean {
        scope.launch {
            try {
                performHealthCheck(params)
            } catch (e: Exception) {
                // Handle error gracefully
                jobFinished(params, false)
            }
        }
        return true // Job is running asynchronously
    }
    
    override fun onStopJob(params: JobParameters?): Boolean {
        scope.cancel()
        return false // Don't reschedule if stopped
    }
    
    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
    
    private suspend fun performHealthCheck(params: JobParameters?) {
        try {
            // Check service connection state
            val isServiceHealthy = isServiceHealthy()
            
            if (!isServiceHealthy) {
                // Attempt recovery if service is unhealthy
                val recoveryResult = serviceRecoveryManager.attemptServiceRecovery()
                
                when (recoveryResult) {
                    is ServiceRecoveryManager.RecoveryResult.SUCCESS -> {
                        // Recovery successful
                    }
                    is ServiceRecoveryManager.RecoveryResult.MAX_RETRIES_EXCEEDED -> {
                        // Too many recovery attempts - might need manual intervention
                        handleRecoveryFailure()
                    }
                    else -> {
                        // Other recovery issues - log and continue monitoring
                    }
                }
            }
            
            // Perform additional health checks
            checkServiceMetrics()
            
        } finally {
            // Job completed successfully
            jobFinished(params, false)
        }
    }
    
    private fun isServiceHealthy(): Boolean {
        val connectionState = audioServiceManager.connectionState.value
        return connectionState == me.shadykhalifa.whispertop.domain.services.IAudioServiceManager.ServiceConnectionState.CONNECTED
    }
    
    private fun checkServiceMetrics() {
        try {
            // Get service health metrics
            val metrics = serviceRecoveryManager.getServiceHealthMetrics()
            
            val consecutiveFailures = metrics["consecutiveFailures"] as? Int ?: 0
            val uptime = metrics["uptime"] as? Long ?: 0L
            val crashCount = metrics["crashCount"] as? Int ?: 0
            
            // Check for concerning patterns
            if (consecutiveFailures > 3) {
                // Too many consecutive failures - might indicate a deeper issue
                handleConsecutiveFailures(consecutiveFailures)
            }
            
            if (crashCount > 5) {
                // Too many crashes - might need to adjust recovery strategy
                handleExcessiveCrashes(crashCount)
            }
            
            // Check if service has been running reliably
            val minHealthyUptime = 10 * 60 * 1000L // 10 minutes
            if (uptime < minHealthyUptime && consecutiveFailures > 0) {
                // Service is not staying up long enough
                handleInstability(uptime, consecutiveFailures)
            }
            
        } catch (e: Exception) {
            // Handle metrics collection error
        }
    }
    
    private fun handleRecoveryFailure() {
        // Could trigger a user notification or other recovery strategies
        // For now, we'll just reset the recovery manager
        serviceRecoveryManager.reset()
    }
    
    private fun handleConsecutiveFailures(failures: Int) {
        // Could implement escalating recovery strategies
        // For now, just track the issue
    }
    
    private fun handleExcessiveCrashes(crashes: Int) {
        // Could implement crash pattern analysis or user notification
        // For now, just track the issue
    }
    
    private fun handleInstability(uptime: Long, failures: Int) {
        // Could implement more aggressive recovery or delay strategies
        // For now, just track the issue
    }
}

/**
 * Extension function to easily schedule health checks from anywhere in the app
 */
fun Context.scheduleServiceHealthCheck() {
    ServiceHealthCheckJob.schedulePeriodicHealthCheck(this)
}

/**
 * Extension function to cancel health checks
 */
fun Context.cancelServiceHealthCheck() {
    ServiceHealthCheckJob.cancelHealthCheck(this)
}