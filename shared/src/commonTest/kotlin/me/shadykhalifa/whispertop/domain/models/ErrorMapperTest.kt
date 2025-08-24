package me.shadykhalifa.whispertop.domain.models

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.audio.AudioRecordingError
import me.shadykhalifa.whispertop.data.models.OpenAIException
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ErrorMapperTest {
    
    private lateinit var errorLoggingService: ErrorLoggingService
    private lateinit var errorMapper: ErrorMapper
    
    @BeforeTest
    fun setup() {
        errorLoggingService = mockk(relaxed = true)
        errorMapper = ErrorMapperImpl(errorLoggingService)
    }
    
    @Test
    fun `mapToErrorInfo should classify TranscriptionError correctly`() = runTest {
        val error = TranscriptionError.ApiKeyMissing()
        
        val result = errorMapper.mapToErrorInfo(error, "test_operation")
        
        assertEquals("API Key Required", result.title)
        assertEquals("Please configure your OpenAI API key in settings to use transcription.", result.message)
        assertEquals("Open Settings", result.actionText)
        assertEquals(ErrorSeverity.CRITICAL, result.severity)
    }
    
    @Test
    fun `mapToErrorInfo should classify AudioRecordingError correctly`() = runTest {
        val error = me.shadykhalifa.whispertop.data.audio.AudioRecordingError.PermissionDenied()
        
        val result = errorMapper.mapToErrorInfo(error, "recording_start")
        
        assertEquals("Microphone Permission Required", result.title)
        assertEquals("WhisperTop needs microphone access to record audio for transcription.", result.message)
        assertEquals("Grant Permission", result.actionText)
        assertEquals(ErrorSeverity.CRITICAL, result.severity)
    }
    
    @Test
    fun `mapToErrorInfo should classify OpenAIException correctly`() = runTest {
        val error = me.shadykhalifa.whispertop.data.models.OpenAIException.RateLimitException("Rate limit exceeded")
        
        val result = errorMapper.mapToErrorInfo(error, "transcription_api")
        
        assertEquals("Rate Limit Exceeded", result.title)
        assertEquals("API rate limit reached. Please wait before making more requests.", result.message)
        assertEquals("Retry", result.actionText)
        assertTrue(result.isRetryable)
        assertEquals(15000L, result.retryDelay)
        assertEquals(ErrorSeverity.WARNING, result.severity)
    }
    
    @Test
    fun `mapToErrorInfo should handle generic exceptions`() = runTest {
        val error = RuntimeException("Generic error message")
        
        val result = errorMapper.mapToErrorInfo(error, "generic_operation")
        
        assertEquals("Unexpected Error", result.title)
        assertEquals("Generic error message", result.message)
        assertEquals("Retry", result.actionText)
        assertTrue(result.isRetryable)
        assertEquals(2000L, result.retryDelay)
        assertEquals(ErrorSeverity.ERROR, result.severity)
    }
    
    @Test
    fun `mapToErrorInfoWithContext should use ErrorContext correctly`() = runTest {
        val error = TranscriptionError.NetworkError(RuntimeException("Network failed"))
        val context = ErrorContext(
            operationName = "transcription_network",
            userId = "user123",
            sessionId = "session456",
            additionalData = mapOf("attempt" to 3, "endpoint" to "api.openai.com")
        )
        
        val result = errorMapper.mapToErrorInfoWithContext(error, context)
        
        assertEquals("Connection Error", result.title)
        assertTrue(result.isRetryable)
        assertEquals(ErrorSeverity.ERROR, result.severity)
    }
    
    @Test
    fun `mapToErrorInfo should log error with context`() = runTest {
        val error = TranscriptionError.AuthenticationError("Invalid API key")
        val context = "api_authentication"
        
        errorMapper.mapToErrorInfo(error, context)
        
        verify {
            errorLoggingService.logError(
                error = error,
                context = match { map ->
                    map["operation"] == context &&
                    map.containsKey("timestamp")
                },
                additionalInfo = "Mapped error from $context"
            )
        }
    }
    
    @Test
    fun `mapToErrorInfoWithContext should log detailed context`() = runTest {
        val error = me.shadykhalifa.whispertop.data.audio.AudioRecordingError.DeviceUnavailable()
        val context = ErrorContext(
            operationName = "audio_recording",
            userId = "user789",
            sessionId = "session123",
            additionalData = mapOf("device" to "builtin_mic", "retry_count" to 2)
        )
        
        errorMapper.mapToErrorInfoWithContext(error, context)
        
        verify {
            errorLoggingService.logError(
                error = error,
                context = match { map ->
                    map["operation"] == "audio_recording" &&
                    map["user_id"] == "user789" &&
                    map["session_id"] == "session123" &&
                    map["device"] == "builtin_mic" &&
                    map["retry_count"] == "2" &&
                    map.containsKey("timestamp")
                },
                additionalInfo = "Mapped error from audio_recording"
            )
        }
    }
    
    @Test
    fun `mapToErrorInfo should handle null error message gracefully`() = runTest {
        val error = RuntimeException(null as String?)
        
        val result = errorMapper.mapToErrorInfo(error, "null_message_test")
        
        assertEquals("Unexpected Error", result.title)
        assertEquals("An unexpected error occurred. Please try again.", result.message)
        assertTrue(result.isRetryable)
    }
    
    @Test
    fun `should preserve error classification logic from ErrorClassifier`() = runTest {
        // Test that ErrorMapper delegates to ErrorClassifier correctly
        val networkError = TranscriptionError.NetworkError(RuntimeException("Connection timeout"))
        val authError = me.shadykhalifa.whispertop.data.models.OpenAIException.AuthenticationException("Invalid key")
        
        val networkResult = errorMapper.mapToErrorInfo(networkError)
        val authResult = errorMapper.mapToErrorInfo(authError)
        
        // Verify network error classification
        assertEquals("Connection Error", networkResult.title)
        assertTrue(networkResult.isRetryable)
        assertEquals(3000L, networkResult.retryDelay)
        
        // Verify auth error classification  
        assertEquals("API Authentication Failed", authResult.title)
        assertEquals(ErrorSeverity.CRITICAL, authResult.severity)
    }
}