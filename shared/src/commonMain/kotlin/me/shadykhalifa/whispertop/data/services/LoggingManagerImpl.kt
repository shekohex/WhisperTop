package me.shadykhalifa.whispertop.data.services

import me.shadykhalifa.whispertop.domain.models.*
import me.shadykhalifa.whispertop.domain.services.LoggingConfig
import me.shadykhalifa.whispertop.domain.services.LoggingManager


/**
 * Default implementation of LoggingManager with in-memory storage and console output
 */
class LoggingManagerImpl(
    override var config: LoggingConfig = LoggingConfig()
) : LoggingManager {
    
    private val logEntries = mutableListOf<LogEntry>()
    

    
    override fun log(
        level: LogLevel,
        component: String,
        message: String,
        context: LogContext?,
        exception: Throwable?,
        performanceMetrics: PerformanceMetrics?
    ) {
        if (!shouldLog(level)) return
        
        val logEntry = LogEntry(
            timestamp = getCurrentTimeMillis(),
            level = level,
            component = component,
            message = message,
            threadInfo = if (config.enableThreadInfo) ThreadInfo.current() else null,
            sessionId = config.sessionId,
            metadata = context?.toMetadata() ?: emptyMap(),
            exception = exception?.let { ExceptionInfo.from(it) },
            performanceMetrics = performanceMetrics
        )
        
        addLogEntry(logEntry)
        
        // Output to console if enabled
        if (config.enableConsoleOutput) {
            outputToConsole(logEntry)
        }
        
        // Platform-specific file output would be implemented here
        if (config.enableFileOutput) {
            outputToFile(logEntry)
        }
    }
    
    override fun getRecentLogs(limit: Int): List<LogEntry> {
        return getLogEntriesSafe().takeLast(limit)
    }
    
    override fun clearLogs() {
        clearLogEntriesSafe()
    }
    
    private fun addLogEntry(logEntry: LogEntry) {
        logEntries.add(logEntry)
        
        // Rotate logs if necessary
        if (logEntries.size > config.maxLogEntries) {
            val toRemove = logEntries.size - config.maxLogEntries
            repeat(toRemove) {
                logEntries.removeAt(0)
            }
        }
    }
    
    private fun getLogEntriesSafe(): List<LogEntry> {
        return logEntries.toList()
    }
    
    private fun clearLogEntriesSafe() {
        logEntries.clear()
    }
    
    override fun updateConfig(newConfig: LoggingConfig) {
        this.config = newConfig
    }
    
    override fun shouldLog(level: LogLevel): Boolean {
        return level.shouldLog(config.minimumLevel)
    }
    
    private fun outputToConsole(logEntry: LogEntry) {
        val timestamp = formatTimestamp(logEntry.timestamp)
        val levelTag = "[${logEntry.level.name}]"
        val componentTag = "[${logEntry.component}]"
        val threadInfo = logEntry.threadInfo?.let { "[${it.threadName}]" } ?: ""
        
        val baseMessage = "$timestamp $levelTag $componentTag $threadInfo ${logEntry.message}"
        
        println(baseMessage)
        
        // Print exception details if present
        logEntry.exception?.let { exception ->
            println("  Exception: ${exception.type} - ${exception.message}")
            if (logEntry.level.priority >= LogLevel.ERROR.priority) {
                println("  Stack trace:")
                exception.stackTrace.lines().take(10).forEach { line ->
                    println("    $line")
                }
            }
        }
        
        // Print performance metrics if present
        logEntry.performanceMetrics?.let { metrics ->
            println("  Performance: ${metrics.operationName} took ${metrics.durationMs}ms")
            metrics.memoryUsageMb?.let { memory ->
                println("  Memory usage: ${memory}MB")
            }
        }
        
        // Print metadata if present and level is DEBUG or higher
        if (logEntry.metadata.isNotEmpty() && logEntry.level.priority >= LogLevel.DEBUG.priority) {
            println("  Metadata: ${logEntry.metadata}")
        }
    }
    
    private fun outputToFile(logEntry: LogEntry) {
        // Platform-specific file output implementation would go here
        // For now, this is a placeholder
    }
    
    private fun formatTimestamp(timestamp: Long): String {
        // Simple timestamp formatting - could be enhanced with actual date formatting
        return timestamp.toString()
    }
}

/**
 * Performance tracking utility for measuring operation durations
 */
class PerformanceTracker(
    private val operationName: String,
    private val component: String,
    private val context: LogContext? = null
) {
    private val startTime = getCurrentTimeMillis()
    private var startMemory: Double? = null
    
    init {
        // Try to capture initial memory usage if available
        startMemory = try {
            getMemoryUsage()
        } catch (e: Exception) {
            null
        }
    }
    
    fun finish(additionalMetrics: Map<String, String> = emptyMap()) {
        val endTime = getCurrentTimeMillis()
        val endMemory = try {
            getMemoryUsage()
        } catch (e: Exception) {
            null
        }
        
        val metrics = PerformanceMetrics(
            operationName = operationName,
            startTime = startTime,
            endTime = endTime,
            memoryUsageMb = endMemory,
            additionalMetrics = additionalMetrics
        )
        
        me.shadykhalifa.whispertop.domain.services.Logger.performance(component, metrics, context)
    }
}

// Platform-specific implementations
expect fun getMemoryUsage(): Double
expect fun getCurrentTimeMillis(): Long