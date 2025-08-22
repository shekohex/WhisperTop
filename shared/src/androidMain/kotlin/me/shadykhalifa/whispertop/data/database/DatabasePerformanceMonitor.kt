package me.shadykhalifa.whispertop.data.database

import android.util.Log
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.system.measureTimeMillis

/**
 * Monitors and tracks database performance metrics for SQLCipher operations
 */
object DatabasePerformanceMonitor {
    private const val TAG = "DBPerformance"
    
    private val mutex = Mutex()
    private val metrics = mutableMapOf<String, OperationMetrics>()
    
    data class OperationMetrics(
        var totalExecutions: Long = 0,
        var totalTime: Long = 0,
        var minTime: Long = Long.MAX_VALUE,
        var maxTime: Long = 0,
        var lastExecutionTime: Long = 0,
        var recentExecutions: MutableList<Long> = mutableListOf()
    ) {
        val averageTime: Double
            get() = if (totalExecutions > 0) totalTime.toDouble() / totalExecutions else 0.0
            
        val recentAverageTime: Double
            get() = if (recentExecutions.isNotEmpty()) recentExecutions.average() else 0.0
    }
    
    /**
     * Execute an operation and measure its performance
     */
    suspend inline fun <T> measureOperation(
        operationName: String,
        operation: () -> T
    ): T {
        val startTime = System.currentTimeMillis()
        
        return try {
            val result: T
            val executionTime = measureTimeMillis {
                result = operation()
            }
            
            recordMetrics(operationName, executionTime, true)
            result
        } catch (e: Exception) {
            val executionTime = System.currentTimeMillis() - startTime
            recordMetrics(operationName, executionTime, false)
            throw e
        }
    }
    
    suspend fun recordMetrics(operationName: String, executionTime: Long, success: Boolean) {
        mutex.withLock {
            val metric = metrics.getOrPut(operationName) { OperationMetrics() }
            
            metric.totalExecutions++
            metric.totalTime += executionTime
            metric.minTime = minOf(metric.minTime, executionTime)
            metric.maxTime = maxOf(metric.maxTime, executionTime)
            metric.lastExecutionTime = executionTime
            
            // Keep only recent 20 executions for rolling average
            metric.recentExecutions.add(executionTime)
            if (metric.recentExecutions.size > 20) {
                metric.recentExecutions.removeAt(0)
            }
            
            // Log slow operations (over 100ms)
            if (executionTime > 100) {
                Log.w(TAG, "Slow database operation: $operationName took ${executionTime}ms")
            }
            
            // Log performance metrics every 50 operations
            if (metric.totalExecutions % 50L == 0L) {
                logPerformanceMetrics(operationName, metric)
            }
        }
    }
    
    private fun logPerformanceMetrics(operationName: String, metric: OperationMetrics) {
        Log.i(TAG, """
            |Performance Metrics for '$operationName':
            |  Total Executions: ${metric.totalExecutions}
            |  Average Time: ${String.format("%.2f", metric.averageTime)}ms
            |  Recent Average: ${String.format("%.2f", metric.recentAverageTime)}ms
            |  Min/Max Time: ${metric.minTime}ms / ${metric.maxTime}ms
            |  Last Execution: ${metric.lastExecutionTime}ms
        """.trimMargin())
    }
    
    /**
     * Get performance summary for all tracked operations
     */
    suspend fun getPerformanceSummary(): Map<String, OperationMetrics> {
        return mutex.withLock {
            metrics.toMap()
        }
    }
    
    /**
     * Log current performance summary
     */
    suspend fun logPerformanceSummary() {
        mutex.withLock {
            Log.i(TAG, "=== Database Performance Summary ===")
            metrics.forEach { (operation, metric) ->
                Log.i(TAG, "$operation: ${metric.totalExecutions} ops, avg ${String.format("%.2f", metric.averageTime)}ms")
            }
            Log.i(TAG, "===================================")
        }
    }
    
    /**
     * Clear all performance metrics
     */
    suspend fun clearMetrics() {
        mutex.withLock {
            metrics.clear()
            Log.d(TAG, "Performance metrics cleared")
        }
    }
    
    /**
     * Monitor encryption overhead by comparing encrypted vs unencrypted operation times
     */
    suspend fun measureEncryptionOverhead(
        operationName: String,
        encryptedOperation: () -> Unit,
        unencryptedOperation: (() -> Unit)? = null
    ) {
        val encryptedTime = measureTimeMillis {
            encryptedOperation()
        }
        
        unencryptedOperation?.let { unencrypted ->
            val unencryptedTime = measureTimeMillis {
                unencrypted()
            }
            
            val overhead = ((encryptedTime - unencryptedTime).toDouble() / unencryptedTime * 100)
            Log.i(TAG, "$operationName encryption overhead: ${String.format("%.1f", overhead)}% ($encryptedTime vs ${unencryptedTime}ms)")
        }
        
        recordMetrics("encrypted_$operationName", encryptedTime, true)
    }
}