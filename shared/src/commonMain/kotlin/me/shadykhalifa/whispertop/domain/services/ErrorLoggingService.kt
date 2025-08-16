package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.ErrorCategory
import me.shadykhalifa.whispertop.domain.models.ErrorClassifier
import me.shadykhalifa.whispertop.domain.models.ErrorSeverity

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
            timestamp = System.currentTimeMillis(),
            severity = errorInfo.severity,
            category = category,
            errorType = error::class.simpleName ?: "Unknown",
            message = error.message ?: "No message",
            stackTrace = error.stackTraceToString(),
            context = enrichedContext,
            sessionId = sessionId
        )
        
        synchronized(errorLogs) {
            errorLogs.add(logEntry)
            if (errorLogs.size > maxLogEntries) {
                errorLogs.removeAt(0)
            }
        }
        
        // Print to console for development
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
            timestamp = System.currentTimeMillis(),
            severity = ErrorSeverity.WARNING,
            category = ErrorCategory.UNKNOWN,
            errorType = "Warning",
            message = message,
            stackTrace = null,
            context = context,
            sessionId = sessionId
        )
        
        synchronized(errorLogs) {
            errorLogs.add(logEntry)
            if (errorLogs.size > maxLogEntries) {
                errorLogs.removeAt(0)
            }
        }
        
        println("[WARNING] $message")
    }
    
    override fun getRecentErrors(limit: Int): List<ErrorLogEntry> {
        synchronized(errorLogs) {
            return errorLogs.takeLast(limit)
        }
    }
    
    override fun clearLogs() {
        synchronized(errorLogs) {
            errorLogs.clear()
        }
    }
    
    companion object {
        private fun generateSessionId(): String {
            return "session_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }
    }
}