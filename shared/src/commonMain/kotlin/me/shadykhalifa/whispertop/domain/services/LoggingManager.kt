package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.*

/**
 * Configuration for logging system behavior
 */
data class LoggingConfig(
    val minimumLevel: LogLevel = LogLevel.DEBUG,
    val enableConsoleOutput: Boolean = true,
    val enableFileOutput: Boolean = false,
    val maxLogEntries: Int = 1000,
    val enablePerformanceMetrics: Boolean = true,
    val enableThreadInfo: Boolean = true,
    val rotateLogsPeriodically: Boolean = true,
    val sessionId: String = generateSessionId()
)

/**
 * Interface for centralized logging management across the application
 */
interface LoggingManager {
    val config: LoggingConfig
    
    /**
     * Log a message at specified level
     */
    fun log(
        level: LogLevel,
        component: String,
        message: String,
        context: LogContext? = null,
        exception: Throwable? = null,
        performanceMetrics: PerformanceMetrics? = null
    )
    
    /**
     * Convenience methods for different log levels
     */
    fun verbose(component: String, message: String, context: LogContext? = null) {
        log(LogLevel.VERBOSE, component, message, context)
    }
    
    fun debug(component: String, message: String, context: LogContext? = null) {
        log(LogLevel.DEBUG, component, message, context)
    }
    
    fun info(component: String, message: String, context: LogContext? = null) {
        log(LogLevel.INFO, component, message, context)
    }
    
    fun warn(component: String, message: String, context: LogContext? = null, exception: Throwable? = null) {
        log(LogLevel.WARN, component, message, context, exception)
    }
    
    fun error(component: String, message: String, context: LogContext? = null, exception: Throwable? = null) {
        log(LogLevel.ERROR, component, message, context, exception)
    }
    
    fun critical(component: String, message: String, context: LogContext? = null, exception: Throwable? = null) {
        log(LogLevel.CRITICAL, component, message, context, exception)
    }
    
    /**
     * Performance tracking methods
     */
    fun logPerformance(component: String, performanceMetrics: PerformanceMetrics, context: LogContext? = null) {
        log(LogLevel.INFO, component, "Performance: ${performanceMetrics.operationName}", context, null, performanceMetrics)
    }
    
    /**
     * Get recent log entries
     */
    fun getRecentLogs(limit: Int = 50): List<LogEntry>
    
    /**
     * Clear all logs
     */
    fun clearLogs()
    
    /**
     * Update logging configuration
     */
    fun updateConfig(newConfig: LoggingConfig)
    
    /**
     * Check if a log level should be logged
     */
    fun shouldLog(level: LogLevel): Boolean {
        return level.shouldLog(config.minimumLevel)
    }
}

/**
 * Facade for easy logging access throughout the application
 */
object Logger {
    private lateinit var manager: LoggingManager
    
    fun initialize(loggingManager: LoggingManager) {
        manager = loggingManager
    }
    
    fun verbose(component: String, message: String, context: LogContext? = null) {
        if (::manager.isInitialized) manager.verbose(component, message, context)
    }
    
    fun debug(component: String, message: String, context: LogContext? = null) {
        if (::manager.isInitialized) manager.debug(component, message, context)
    }
    
    fun info(component: String, message: String, context: LogContext? = null) {
        if (::manager.isInitialized) manager.info(component, message, context)
    }
    
    fun warn(component: String, message: String, context: LogContext? = null, exception: Throwable? = null) {
        if (::manager.isInitialized) manager.warn(component, message, context, exception)
    }
    
    fun error(component: String, message: String, context: LogContext? = null, exception: Throwable? = null) {
        if (::manager.isInitialized) manager.error(component, message, context, exception)
    }
    
    fun critical(component: String, message: String, context: LogContext? = null, exception: Throwable? = null) {
        if (::manager.isInitialized) manager.critical(component, message, context, exception)
    }
    
    fun performance(component: String, performanceMetrics: PerformanceMetrics, context: LogContext? = null) {
        if (::manager.isInitialized) manager.logPerformance(component, performanceMetrics, context)
    }
    
    fun getRecentLogs(limit: Int = 50): List<LogEntry> {
        return if (::manager.isInitialized) manager.getRecentLogs(limit) else emptyList()
    }
    
    fun shouldLog(level: LogLevel): Boolean {
        return if (::manager.isInitialized) manager.shouldLog(level) else false
    }
}

/**
 * Extension function to create a logger for a specific component
 */
fun Any.createLogger(): ComponentLogger {
    return ComponentLogger(this::class.simpleName ?: "Unknown")
}

/**
 * Component-specific logger that automatically includes component name
 */
class ComponentLogger(private val componentName: String) {
    fun verbose(message: String, context: LogContext? = null) {
        Logger.verbose(componentName, message, context)
    }
    
    fun debug(message: String, context: LogContext? = null) {
        Logger.debug(componentName, message, context)
    }
    
    fun info(message: String, context: LogContext? = null) {
        Logger.info(componentName, message, context)
    }
    
    fun warn(message: String, context: LogContext? = null, exception: Throwable? = null) {
        Logger.warn(componentName, message, context, exception)
    }
    
    fun error(message: String, context: LogContext? = null, exception: Throwable? = null) {
        Logger.error(componentName, message, context, exception)
    }
    
    fun critical(message: String, context: LogContext? = null, exception: Throwable? = null) {
        Logger.critical(componentName, message, context, exception)
    }
    
    fun performance(performanceMetrics: PerformanceMetrics, context: LogContext? = null) {
        Logger.performance(componentName, performanceMetrics, context)
    }
}

private fun generateSessionId(): String {
    return "session_${getCurrentTimeMillis()}_${(1000..9999).random()}"
}

// Platform-specific implementation
expect fun getCurrentTimeMillis(): Long