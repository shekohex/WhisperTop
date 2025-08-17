package me.shadykhalifa.whispertop.data.services

import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.LogLevel
import me.shadykhalifa.whispertop.domain.services.LoggingConfig
import me.shadykhalifa.whispertop.domain.services.LoggingManager

/**
 * Factory for creating and configuring logging managers
 */
object LoggingFactory {
    
    /**
     * Create a logging manager from app settings
     */
    fun createLoggingManager(
        appSettings: AppSettings,
        isDebugBuild: Boolean = true
    ): LoggingManager {
        val logLevel = LogLevel.fromString(appSettings.logLevel) 
            ?: if (isDebugBuild) LogLevel.getDebugDefault() else LogLevel.getReleaseDefault()
        
        val config = LoggingConfig(
            minimumLevel = logLevel,
            enableConsoleOutput = appSettings.enableConsoleLogging,
            enableFileOutput = appSettings.enableFileLogging,
            maxLogEntries = appSettings.maxLogEntries,
            enablePerformanceMetrics = appSettings.enablePerformanceMetrics,
            enableThreadInfo = isDebugBuild, // Only enable thread info in debug builds
            rotateLogsPeriodically = true
        )
        
        return LoggingManagerImpl(config)
    }
    
    /**
     * Create a default logging manager for development
     */
    fun createDefaultLoggingManager(): LoggingManager {
        return LoggingManagerImpl(
            LoggingConfig(
                minimumLevel = LogLevel.DEBUG,
                enableConsoleOutput = true,
                enableFileOutput = false,
                enablePerformanceMetrics = true,
                enableThreadInfo = true
            )
        )
    }
    
    /**
     * Create a minimal logging manager for production
     */
    fun createProductionLoggingManager(): LoggingManager {
        return LoggingManagerImpl(
            LoggingConfig(
                minimumLevel = LogLevel.WARN,
                enableConsoleOutput = false,
                enableFileOutput = true,
                enablePerformanceMetrics = false,
                enableThreadInfo = false,
                maxLogEntries = 500
            )
        )
    }
    
    /**
     * Update logging configuration based on new app settings
     */
    fun updateLoggingConfig(
        loggingManager: LoggingManager,
        appSettings: AppSettings,
        isDebugBuild: Boolean = true
    ) {
        val logLevel = LogLevel.fromString(appSettings.logLevel) 
            ?: if (isDebugBuild) LogLevel.getDebugDefault() else LogLevel.getReleaseDefault()
        
        val newConfig = LoggingConfig(
            minimumLevel = logLevel,
            enableConsoleOutput = appSettings.enableConsoleLogging,
            enableFileOutput = appSettings.enableFileLogging,
            maxLogEntries = appSettings.maxLogEntries,
            enablePerformanceMetrics = appSettings.enablePerformanceMetrics,
            enableThreadInfo = isDebugBuild,
            rotateLogsPeriodically = true,
            sessionId = loggingManager.config.sessionId // Keep existing session ID
        )
        
        loggingManager.updateConfig(newConfig)
    }
}