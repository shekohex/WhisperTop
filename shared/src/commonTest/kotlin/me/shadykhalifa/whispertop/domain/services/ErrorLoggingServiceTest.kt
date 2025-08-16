package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.ErrorCategory
import me.shadykhalifa.whispertop.domain.models.ErrorSeverity
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.data.audio.AudioRecordingError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull

class ErrorLoggingServiceTest {

    private val errorLoggingService = ErrorLoggingServiceImpl("test_session")

    @Test
    fun `logError should create error log entry with correct information`() {
        val error = TranscriptionError.ApiKeyMissing()
        val context = mapOf("screen" to "settings", "action" to "save_api_key")
        
        errorLoggingService.logError(error, context, "Additional info")
        
        val recentErrors = errorLoggingService.getRecentErrors(1)
        assertEquals(1, recentErrors.size)
        
        val logEntry = recentErrors.first()
        assertEquals(ErrorSeverity.CRITICAL, logEntry.severity)
        assertEquals(ErrorCategory.AUTHENTICATION, logEntry.category)
        assertEquals("ApiKeyMissing", logEntry.errorType)
        assertEquals(error.message, logEntry.message)
        assertEquals("test_session", logEntry.sessionId)
        assertNotNull(logEntry.stackTrace)
        
        // Check enriched context
        assertTrue(logEntry.context.containsKey("screen"))
        assertTrue(logEntry.context.containsKey("action"))
        assertTrue(logEntry.context.containsKey("additional_info"))
        assertTrue(logEntry.context.containsKey("error_title"))
        assertTrue(logEntry.context.containsKey("retryable"))
        
        assertEquals("settings", logEntry.context["screen"])
        assertEquals("Additional info", logEntry.context["additional_info"])
        assertEquals("API Key Required", logEntry.context["error_title"])
        assertEquals("false", logEntry.context["retryable"])
    }

    @Test
    fun `logError should handle different error types correctly`() {
        val networkError = TranscriptionError.NetworkError(RuntimeException("Connection failed"))
        val audioError = AudioRecordingError.DeviceUnavailable()
        val genericError = RuntimeException("Unknown error")
        
        errorLoggingService.logError(networkError)
        errorLoggingService.logError(audioError)
        errorLoggingService.logError(genericError)
        
        val recentErrors = errorLoggingService.getRecentErrors(10)
        assertEquals(3, recentErrors.size)
        
        // Check network error
        val networkLog = recentErrors.find { it.category == ErrorCategory.NETWORK }
        assertNotNull(networkLog)
        assertEquals(ErrorSeverity.ERROR, networkLog.severity)
        assertEquals("true", networkLog.context["retryable"])
        
        // Check audio error
        val audioLog = recentErrors.find { it.category == ErrorCategory.AUDIO }
        assertNotNull(audioLog)
        assertEquals("DeviceUnavailable", audioLog.errorType)
        
        // Check generic error
        val genericLog = recentErrors.find { it.errorType == "RuntimeException" }
        assertNotNull(genericLog)
        assertEquals(ErrorCategory.UNKNOWN, genericLog.category)
    }

    @Test
    fun `logWarning should create warning log entry`() {
        val message = "This is a warning message"
        val context = mapOf("component" to "audio_recorder")
        
        errorLoggingService.logWarning(message, context)
        
        val recentErrors = errorLoggingService.getRecentErrors(1)
        assertEquals(1, recentErrors.size)
        
        val logEntry = recentErrors.first()
        assertEquals(ErrorSeverity.WARNING, logEntry.severity)
        assertEquals(ErrorCategory.UNKNOWN, logEntry.category)
        assertEquals("Warning", logEntry.errorType)
        assertEquals(message, logEntry.message)
        assertEquals("test_session", logEntry.sessionId)
        assertEquals(null, logEntry.stackTrace)
        assertEquals("audio_recorder", logEntry.context["component"])
    }

    @Test
    fun `getRecentErrors should respect limit parameter`() {
        // Add 5 errors
        repeat(5) { index ->
            errorLoggingService.logError(RuntimeException("Error $index"))
        }
        
        val limitedErrors = errorLoggingService.getRecentErrors(3)
        assertEquals(3, limitedErrors.size)
        
        val allErrors = errorLoggingService.getRecentErrors(10)
        assertEquals(5, allErrors.size)
    }

    @Test
    fun `getRecentErrors should return errors in chronological order`() {
        val error1 = RuntimeException("First error")
        val error2 = RuntimeException("Second error")
        val error3 = RuntimeException("Third error")
        
        errorLoggingService.logError(error1)
        Thread.sleep(10) // Ensure different timestamps
        errorLoggingService.logError(error2)
        Thread.sleep(10)
        errorLoggingService.logError(error3)
        
        val recentErrors = errorLoggingService.getRecentErrors(10)
        assertEquals(3, recentErrors.size)
        
        // Should be in chronological order (oldest first)
        assertTrue(recentErrors[0].timestamp <= recentErrors[1].timestamp)
        assertTrue(recentErrors[1].timestamp <= recentErrors[2].timestamp)
        
        assertEquals("First error", recentErrors[0].message)
        assertEquals("Second error", recentErrors[1].message)
        assertEquals("Third error", recentErrors[2].message)
    }

    @Test
    fun `clearLogs should remove all log entries`() {
        // Add several errors
        repeat(3) { index ->
            errorLoggingService.logError(RuntimeException("Error $index"))
        }
        
        val beforeClear = errorLoggingService.getRecentErrors(10)
        assertEquals(3, beforeClear.size)
        
        errorLoggingService.clearLogs()
        
        val afterClear = errorLoggingService.getRecentErrors(10)
        assertEquals(0, afterClear.size)
    }

    @Test
    fun `logError should handle errors without messages`() {
        val errorWithoutMessage = object : RuntimeException() {
            override val message: String? = null
        }
        
        errorLoggingService.logError(errorWithoutMessage)
        
        val recentErrors = errorLoggingService.getRecentErrors(1)
        assertEquals(1, recentErrors.size)
        
        val logEntry = recentErrors.first()
        assertEquals("No message", logEntry.message)
    }

    @Test
    fun `error logging should handle maximum log entries limit`() {
        val maxEntries = 1000
        
        // Add more than max entries
        repeat(maxEntries + 100) { index ->
            errorLoggingService.logError(RuntimeException("Error $index"))
        }
        
        val allErrors = errorLoggingService.getRecentErrors(Int.MAX_VALUE)
        assertEquals(maxEntries, allErrors.size)
        
        // Should keep the most recent entries
        val lastError = allErrors.last()
        assertTrue(lastError.message.contains("Error ${maxEntries + 99}"))
        
        val firstError = allErrors.first()
        assertTrue(firstError.message.contains("Error 100"))
    }

    @Test
    fun `logError should include stack trace for errors`() {
        val error = RuntimeException("Test error")
        
        errorLoggingService.logError(error)
        
        val recentErrors = errorLoggingService.getRecentErrors(1)
        val logEntry = recentErrors.first()
        
        assertNotNull(logEntry.stackTrace)
        assertTrue(logEntry.stackTrace!!.contains("RuntimeException"))
        assertTrue(logEntry.stackTrace!!.contains("Test error"))
    }

    @Test
    fun `error timestamp should be accurate`() {
        val beforeTime = System.currentTimeMillis()
        
        errorLoggingService.logError(RuntimeException("Timestamp test"))
        
        val afterTime = System.currentTimeMillis()
        val recentErrors = errorLoggingService.getRecentErrors(1)
        val logEntry = recentErrors.first()
        
        assertTrue(logEntry.timestamp >= beforeTime)
        assertTrue(logEntry.timestamp <= afterTime)
    }
}