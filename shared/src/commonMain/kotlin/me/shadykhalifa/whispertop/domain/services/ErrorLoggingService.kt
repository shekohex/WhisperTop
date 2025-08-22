package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.runBlocking
import me.shadykhalifa.whispertop.domain.models.ErrorCategory
import me.shadykhalifa.whispertop.domain.models.ErrorClassifier
import me.shadykhalifa.whispertop.domain.models.ErrorSeverity
import me.shadykhalifa.whispertop.domain.models.getCurrentTimeMillis

data class ErrorLogEntry(
    val timestamp: Long,
    val severity: ErrorSeverity,
    val category: ErrorCategory,
    val errorType: String,
    val message: String,
    val stackTrace: String?,
    val context: Map<String, String> = emptyMap(),
    val sessionId: String
)

interface ErrorLoggingService {
    fun logError(
        error: Throwable, 
        context: Map<String, String> = emptyMap(),
        additionalInfo: String? = null
    )
    
    fun logWarning(
        message: String,
        context: Map<String, String> = emptyMap()
    )
    
    fun getRecentErrors(limit: Int = 50): List<ErrorLogEntry>
    fun clearLogs()
}

class ErrorLoggingServiceImpl(
    private val sessionId: String = generateSessionId()
) : ErrorLoggingService {
    
    private val errorLogs = mutableListOf<ErrorLogEntry>()
    private val maxLogEntries = 1000
    private val errorLogsMutex = Mutex()
    
    override fun logError(
        error: Throwable, 
        context: Map<String, String>,
        additionalInfo: String?
    ) {
        val errorInfo = ErrorClassifier.classifyError(error)
        val category = ErrorClassifier.getCategory(error)
        
        val enrichedContext = context.toMutableMap().apply {
            additionalInfo?.let { put("additional_info", it) }
            put("error_title", errorInfo.title)
            put("retryable", errorInfo.isRetryable.toString())
        }
        
        val logEntry = ErrorLogEntry(
            timestamp = getCurrentTimeMillis(),
            severity = errorInfo.severity,
            category = category,
            errorType = error::class.simpleName ?: "Unknown",
            message = error.message ?: "No message",
            stackTrace = error.stackTraceToString(),
            context = enrichedContext,
            sessionId = sessionId
        )
        
        runBlocking {
            errorLogsMutex.withLock {
                errorLogs.add(logEntry)
                if (errorLogs.size > maxLogEntries) {
                    errorLogs.removeAt(0)
                }
            }
        }
        
        // Use the new logging system
        val logLevel = when (logEntry.severity) {
            ErrorSeverity.WARNING -> me.shadykhalifa.whispertop.domain.models.LogLevel.WARN
            ErrorSeverity.ERROR -> me.shadykhalifa.whispertop.domain.models.LogLevel.ERROR
            ErrorSeverity.CRITICAL -> me.shadykhalifa.whispertop.domain.models.LogLevel.CRITICAL
        }
        
        val context = me.shadykhalifa.whispertop.domain.models.LogContext(
            component = "ErrorLogging",
            additionalContext = logEntry.context + mapOf(
                "error_category" to logEntry.category.name,
                "error_type" to logEntry.errorType,
                "session_id" to logEntry.sessionId
            )
        )
        
        // Use centralized logging if Logger is initialized
        try {
            me.shadykhalifa.whispertop.domain.services.Logger.error("ErrorLogging", logEntry.message, context, error)
        } catch (e: Exception) {
            // Logger not initialized, fall back to console
        }
        
        // Fallback to console for development (in case Logger isn't initialized)
        println("[${logEntry.severity}] ${logEntry.category}: ${logEntry.message}")
        if (logEntry.severity == ErrorSeverity.CRITICAL) {
            println("Stack trace: ${logEntry.stackTrace}")
        }
    }
    
    override fun logWarning(
        message: String,
        context: Map<String, String>
    ) {
        val logEntry = ErrorLogEntry(
            timestamp = getCurrentTimeMillis(),
            severity = ErrorSeverity.WARNING,
            category = ErrorCategory.UNKNOWN,
            errorType = "Warning",
            message = message,
            stackTrace = null,
            context = context,
            sessionId = sessionId
        )
        
        runBlocking {
            errorLogsMutex.withLock {
                errorLogs.add(logEntry)
                if (errorLogs.size > maxLogEntries) {
                    errorLogs.removeAt(0)
                }
            }
        }
        
        // Use centralized logging
        val logContext = me.shadykhalifa.whispertop.domain.models.LogContext(
            component = "ErrorLogging",
            additionalContext = context + mapOf("session_id" to logEntry.sessionId)
        )
        try {
            me.shadykhalifa.whispertop.domain.services.Logger.warn("ErrorLogging", message, logContext)
        } catch (e: Exception) {
            // Logger not initialized, fall back to console
        }
        
        // Fallback to console
        println("[WARNING] $message")
    }
    
    override fun getRecentErrors(limit: Int): List<ErrorLogEntry> {
        return runBlocking {
            errorLogsMutex.withLock {
                errorLogs.takeLast(limit)
            }
        }
    }
    
    override fun clearLogs() {
        runBlocking {
            errorLogsMutex.withLock {
                errorLogs.clear()
            }
        }
    }
    
    companion object {
        private fun generateSessionId(): String {
            return "session_${getCurrentTimeMillis()}_${(1000..9999).random()}"
        }
    }
}