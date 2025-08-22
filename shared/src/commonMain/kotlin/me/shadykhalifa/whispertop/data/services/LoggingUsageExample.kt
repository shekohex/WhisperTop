package me.shadykhalifa.whispertop.data.services

import me.shadykhalifa.whispertop.domain.models.LogContext
import me.shadykhalifa.whispertop.domain.models.PerformanceMetrics
import me.shadykhalifa.whispertop.domain.services.Logger
import me.shadykhalifa.whispertop.domain.services.createLogger

/**
 * Example demonstrating how to use the new comprehensive logging system
 */
class LoggingUsageExample {
    
    // Component-specific logger
    private val logger = createLogger()
    
    fun demonstrateBasicLogging() {
        // Basic logging with different levels
        logger.debug("Starting audio recording process")
        logger.info("Audio recording configuration completed")
        logger.warn("Low battery detected - recording may be interrupted")
        
        // Error logging with exception
        try {
            throw Exception("Simulated error for demo")
        } catch (e: Exception) {
            logger.error("Failed to initialize audio device", exception = e)
        }
    }
    
    fun demonstrateContextualLogging() {
        val context = LogContext(
            operationId = "record_123",
            userId = "user_456",
            requestId = "req_789",
            component = "AudioRecording",
            additionalContext = mapOf(
                "session_duration" to "120s",
                "audio_quality" to "high",
                "device_model" to "Pixel 7"
            )
        )
        
        logger.info("Recording started with high quality settings", context)
        logger.debug("Audio buffer initialized with 1024 samples", context)
    }
    
    suspend fun demonstratePerformanceLogging() {
        // Performance tracking
        val performanceTracker = PerformanceTracker(
            operationName = "audio_transcription",
            component = "TranscriptionService"
        )
        
        // Simulate some work
        kotlinx.coroutines.delay(100) // In real code, this would be actual work
        
        // Finish tracking with additional metrics
        performanceTracker.finish(mapOf(
            "audio_length_seconds" to "5.2",
            "transcription_accuracy" to "95%",
            "model_used" to "whisper-1"
        ))
    }
    
    fun demonstrateDirectLogging() {
        // Direct access to the global Logger
        Logger.verbose("AudioRecording", "Detailed debug information for troubleshooting")
        
        Logger.info("TranscriptionService", "Successfully transcribed audio", LogContext(
            component = "TranscriptionService",
            additionalContext = mapOf("duration_ms" to "1250", "words_count" to "42")
        ))
        
        Logger.critical("SystemHealth", "Critical system error detected", 
            exception = RuntimeException("System memory critically low"))
    }
    
    fun demonstrateLogRetrieval() {
        // Retrieve recent logs for display or analysis
        val recentLogs = Logger.getRecentLogs(limit = 20)
        
        println("Recent log entries:")
        recentLogs.forEach { logEntry ->
            println("${logEntry.timestamp} [${logEntry.level}] ${logEntry.component}: ${logEntry.message}")
            
            // Show performance metrics if available
            logEntry.performanceMetrics?.let { metrics ->
                println("  Performance: ${metrics.operationName} took ${metrics.durationMs}ms")
            }
            
            // Show exception details if available
            logEntry.exception?.let { exception ->
                println("  Exception: ${exception.type} - ${exception.message}")
            }
        }
    }
    
    companion object {
        fun initializeLoggingSystem() {
            // This would typically be done in your Application class or DI setup
            val loggingManager = LoggingFactory.createDefaultLoggingManager()
            Logger.initialize(loggingManager)
            
            Logger.info("LoggingSystem", "Comprehensive logging system initialized successfully")
        }
    }
}