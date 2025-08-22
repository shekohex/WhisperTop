package me.shadykhalifa.whispertop.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Comprehensive error monitoring and alerting system for critical operations
 */
@Serializable
data class ErrorEvent(
    val timestamp: Long,
    val component: String,
    val operation: String,
    val errorType: String,
    val errorMessage: String,
    val severity: ErrorSeverity,
    val context: Map<String, String> = emptyMap(),
    val retryCount: Int = 0
)

@Serializable
enum class ErrorSeverity {
    LOW,        // Minor issues, logging only
    MEDIUM,     // Issues that affect functionality but not critical
    HIGH,       // Critical issues that affect core functionality
    CRITICAL    // System-breaking issues that require immediate attention
}

interface ErrorReporter {
    suspend fun reportError(error: ErrorEvent)
    suspend fun reportMetric(metric: String, value: Double, tags: Map<String, String> = emptyMap())
}

class LoggingErrorReporter : ErrorReporter {
    override suspend fun reportError(error: ErrorEvent) {
        val tag = "ErrorMonitor_${error.component}"
        val message = "${error.operation}: ${error.errorMessage}"
        
        when (error.severity) {
            ErrorSeverity.LOW -> android.util.Log.d(tag, message)
            ErrorSeverity.MEDIUM -> android.util.Log.w(tag, message)
            ErrorSeverity.HIGH -> android.util.Log.e(tag, message)
            ErrorSeverity.CRITICAL -> {
                android.util.Log.wtf(tag, message)
                // In production, this could trigger push notifications or external alerts
            }
        }
    }
    
    override suspend fun reportMetric(metric: String, value: Double, tags: Map<String, String>) {
        android.util.Log.d("Metrics", "$metric: $value ${tags.entries.joinToString { "${it.key}=${it.value}" }}")
    }
}

object ErrorMonitor {
    private val reporters = mutableListOf<ErrorReporter>()
    private val errorHistory = mutableListOf<ErrorEvent>()
    private val maxHistorySize = 1000
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Error rate tracking
    private val errorRates = mutableMapOf<String, MutableList<Long>>()
    private val errorRateWindow = 5 * 60 * 1000L // 5 minutes
    
    init {
        // Add default logging reporter
        addReporter(LoggingErrorReporter())
    }
    
    fun addReporter(reporter: ErrorReporter) {
        reporters.add(reporter)
    }
    
    fun removeReporter(reporter: ErrorReporter) {
        reporters.remove(reporter)
    }
    
    /**
     * Report an error with automatic severity detection and rate limiting
     */
    fun reportError(
        component: String,
        operation: String,
        throwable: Throwable,
        context: Map<String, String> = emptyMap(),
        retryCount: Int = 0
    ) {
        val severity = determineSeverity(throwable, retryCount)
        val errorType = throwable::class.simpleName ?: "Unknown"
        
        val errorEvent = ErrorEvent(
            timestamp = Clock.System.now().toEpochMilliseconds(),
            component = component,
            operation = operation,
            errorType = errorType,
            errorMessage = throwable.message ?: "No message",
            severity = severity,
            context = context,
            retryCount = retryCount
        )
        
        reportError(errorEvent)
    }
    
    /**
     * Report a custom error event
     */
    fun reportError(errorEvent: ErrorEvent) {
        scope.launch {
            try {
                // Add to history
                addToHistory(errorEvent)
                
                // Check error rate
                if (isErrorRateExceeded(errorEvent.component, errorEvent.operation)) {
                    val criticalEvent = errorEvent.copy(
                        severity = ErrorSeverity.CRITICAL,
                        errorMessage = "Error rate exceeded for ${errorEvent.component}.${errorEvent.operation}: ${errorEvent.errorMessage}"
                    )
                    notifyReporters(criticalEvent)
                } else {
                    notifyReporters(errorEvent)
                }
                
                // Track error rate
                trackErrorRate(errorEvent.component, errorEvent.operation)
                
            } catch (e: Exception) {
                // Failsafe: Don't let error reporting break
                android.util.Log.e("ErrorMonitor", "Failed to report error", e)
            }
        }
    }
    
    /**
     * Report performance metrics
     */
    fun reportMetric(metric: String, value: Double, tags: Map<String, String> = emptyMap()) {
        scope.launch {
            try {
                reporters.forEach { reporter ->
                    reporter.reportMetric(metric, value, tags)
                }
            } catch (e: Exception) {
                android.util.Log.e("ErrorMonitor", "Failed to report metric", e)
            }
        }
    }
    
    /**
     * Get recent error events for debugging
     */
    fun getRecentErrors(count: Int = 50): List<ErrorEvent> {
        return errorHistory.takeLast(count)
    }
    
    /**
     * Get error statistics for a component
     */
    fun getErrorStats(component: String, timeWindowMs: Long = 3600000): ErrorStats {
        val now = Clock.System.now().toEpochMilliseconds()
        val recentErrors = errorHistory.filter { 
            it.component == component && (now - it.timestamp) <= timeWindowMs 
        }
        
        return ErrorStats(
            totalErrors = recentErrors.size,
            errorsByType = recentErrors.groupBy { it.errorType }.mapValues { it.value.size },
            errorsBySeverity = recentErrors.groupBy { it.severity }.mapValues { it.value.size },
            averageRetryCount = recentErrors.map { it.retryCount }.average(),
            timeWindow = timeWindowMs
        )
    }
    
    /**
     * Check system health based on error patterns
     */
    fun getSystemHealth(): SystemHealth {
        val now = Clock.System.now().toEpochMilliseconds()
        val recentErrors = errorHistory.filter { (now - it.timestamp) <= 3600000 } // Last hour
        
        val criticalErrors = recentErrors.count { it.severity == ErrorSeverity.CRITICAL }
        val highErrors = recentErrors.count { it.severity == ErrorSeverity.HIGH }
        val totalErrors = recentErrors.size
        
        val health = when {
            criticalErrors > 0 -> SystemHealth.CRITICAL
            highErrors > 10 -> SystemHealth.DEGRADED
            totalErrors > 50 -> SystemHealth.WARNING
            else -> SystemHealth.HEALTHY
        }
        
        return health
    }
    
    private fun addToHistory(errorEvent: ErrorEvent) {
        errorHistory.add(errorEvent)
        if (errorHistory.size > maxHistorySize) {
            errorHistory.removeAt(0)
        }
    }
    
    private suspend fun notifyReporters(errorEvent: ErrorEvent) {
        reporters.forEach { reporter ->
            try {
                reporter.reportError(errorEvent)
            } catch (e: Exception) {
                // Don't let one reporter failure affect others
                android.util.Log.e("ErrorMonitor", "Reporter failed", e)
            }
        }
    }
    
    private fun determineSeverity(throwable: Throwable, retryCount: Int): ErrorSeverity {
        return when {
            throwable is OutOfMemoryError -> ErrorSeverity.CRITICAL
            throwable is SecurityException -> ErrorSeverity.HIGH
            throwable is IllegalStateException && retryCount > 2 -> ErrorSeverity.HIGH
            throwable is NetworkException && retryCount > 3 -> ErrorSeverity.MEDIUM
            retryCount > 5 -> ErrorSeverity.HIGH
            else -> ErrorSeverity.MEDIUM
        }
    }
    
    private fun trackErrorRate(component: String, operation: String) {
        val key = "$component.$operation"
        val now = Clock.System.now().toEpochMilliseconds()
        
        val rates = errorRates.getOrPut(key) { mutableListOf() }
        rates.add(now)
        
        // Clean old entries
        rates.removeAll { (now - it) > errorRateWindow }
    }
    
    private fun isErrorRateExceeded(component: String, operation: String): Boolean {
        val key = "$component.$operation"
        val rates = errorRates[key] ?: return false
        
        // More than 10 errors in 5 minutes is considered excessive
        return rates.size > 10
    }
}

data class ErrorStats(
    val totalErrors: Int,
    val errorsByType: Map<String, Int>,
    val errorsBySeverity: Map<ErrorSeverity, Int>,
    val averageRetryCount: Double,
    val timeWindow: Long
)

enum class SystemHealth {
    HEALTHY,
    WARNING,
    DEGRADED,
    CRITICAL
}

class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause)