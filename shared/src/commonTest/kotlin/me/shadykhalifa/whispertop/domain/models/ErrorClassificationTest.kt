package me.shadykhalifa.whispertop.domain.models

import me.shadykhalifa.whispertop.data.audio.AudioRecordingError
import me.shadykhalifa.whispertop.data.models.OpenAIException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ErrorClassificationTest {

    @Test
    fun `classifyError should classify TranscriptionError correctly`() {
        val apiKeyError = TranscriptionError.ApiKeyMissing()
        val errorInfo = ErrorClassifier.classifyError(apiKeyError)
        
        assertEquals("API Key Required", errorInfo.title)
        assertEquals("Open Settings", errorInfo.actionText)
        assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
        assertFalse(errorInfo.isRetryable)
    }

    @Test
    fun `classifyError should classify network errors as retryable`() {
        val networkError = TranscriptionError.NetworkError(RuntimeException("Connection failed"))
        val errorInfo = ErrorClassifier.classifyError(networkError)
        
        assertEquals("Connection Error", errorInfo.title)
        assertEquals("Retry", errorInfo.actionText)
        assertEquals(ErrorSeverity.ERROR, errorInfo.severity)
        assertTrue(errorInfo.isRetryable)
        assertEquals(3000L, errorInfo.retryDelay)
    }

    @Test
    fun `classifyError should classify rate limit errors with appropriate delay`() {
        val rateLimitError = TranscriptionError.RateLimitError()
        val errorInfo = ErrorClassifier.classifyError(rateLimitError)
        
        assertEquals("Rate Limit Exceeded", errorInfo.title)
        assertEquals("Retry", errorInfo.actionText)
        assertEquals(ErrorSeverity.WARNING, errorInfo.severity)
        assertTrue(errorInfo.isRetryable)
        assertEquals(10000L, errorInfo.retryDelay)
    }

    @Test
    fun `classifyError should classify audio recording errors correctly`() {
        val permissionError = AudioRecordingError.PermissionDenied()
        val errorInfo = ErrorClassifier.classifyError(permissionError)
        
        assertEquals("Microphone Permission Required", errorInfo.title)
        assertEquals("Grant Permission", errorInfo.actionText)
        assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
    }

    @Test
    fun `classifyError should classify device unavailable as retryable`() {
        val deviceError = AudioRecordingError.DeviceUnavailable()
        val errorInfo = ErrorClassifier.classifyError(deviceError)
        
        assertEquals("Microphone Unavailable", errorInfo.title)
        assertTrue(errorInfo.isRetryable)
        assertEquals(2000L, errorInfo.retryDelay)
    }

    @Test
    fun `classifyError should classify OpenAI authentication errors correctly`() {
        val authError = OpenAIException.AuthenticationException("Invalid API key")
        val errorInfo = ErrorClassifier.classifyError(authError)
        
        assertEquals("API Authentication Failed", errorInfo.title)
        assertEquals("Update API Key", errorInfo.actionText)
        assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
        assertFalse(errorInfo.isRetryable)
    }

    @Test
    fun `classifyError should classify OpenAI rate limit with longer delay`() {
        val rateLimitError = OpenAIException.RateLimitException("Rate limit exceeded")
        val errorInfo = ErrorClassifier.classifyError(rateLimitError)
        
        assertEquals("Rate Limit Exceeded", errorInfo.title)
        assertTrue(errorInfo.isRetryable)
        assertEquals(15000L, errorInfo.retryDelay)
    }

    @Test
    fun `classifyError should classify generic security exceptions`() {
        val securityError = SecurityException("Permission denied")
        val errorInfo = ErrorClassifier.classifyError(securityError)
        
        assertEquals("Permission Required", errorInfo.title)
        assertEquals("Grant Permission", errorInfo.actionText)
        assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
    }

    @Test
    fun `classifyError should handle unknown errors gracefully`() {
        val unknownError = RuntimeException("Unknown error")
        val errorInfo = ErrorClassifier.classifyError(unknownError)
        
        assertEquals("Unexpected Error", errorInfo.title)
        assertEquals("Retry", errorInfo.actionText)
        assertTrue(errorInfo.isRetryable)
        assertEquals(2000L, errorInfo.retryDelay)
    }

    @Test
    fun `getCategory should return correct categories`() {
        assertEquals(ErrorCategory.NETWORK, ErrorClassifier.getCategory(TranscriptionError.NetworkError(RuntimeException())))
        assertEquals(ErrorCategory.AUTHENTICATION, ErrorClassifier.getCategory(TranscriptionError.ApiKeyMissing()))
        assertEquals(ErrorCategory.PERMISSION, ErrorClassifier.getCategory(AudioRecordingError.PermissionDenied()))
        assertEquals(ErrorCategory.AUDIO, ErrorClassifier.getCategory(AudioRecordingError.DeviceUnavailable()))
        assertEquals(ErrorCategory.STORAGE, ErrorClassifier.getCategory(TranscriptionError.StorageError()))
        assertEquals(ErrorCategory.API, ErrorClassifier.getCategory(OpenAIException.ServerException("Server error")))
        assertEquals(ErrorCategory.ACCESSIBILITY, ErrorClassifier.getCategory(TranscriptionError.AccessibilityServiceNotEnabled()))
        assertEquals(ErrorCategory.CONFIGURATION, ErrorClassifier.getCategory(TranscriptionError.ServiceNotConfigured()))
        assertEquals(ErrorCategory.UNKNOWN, ErrorClassifier.getCategory(RuntimeException("Unknown")))
    }

    @Test
    fun `classifyError should handle text insertion failures correctly`() {
        val textInsertionError = TranscriptionError.TextInsertionFailed("Test transcription")
        val errorInfo = ErrorClassifier.classifyError(textInsertionError)
        
        assertEquals("Text Insertion Failed", errorInfo.title)
        assertEquals("Copy Text", errorInfo.actionText)
        assertEquals(ErrorSeverity.WARNING, errorInfo.severity)
        assertFalse(errorInfo.isRetryable)
    }

    @Test
    fun `classifyError should handle audio size errors appropriately`() {
        val tooLargeError = TranscriptionError.AudioTooLarge()
        val tooShortError = TranscriptionError.AudioTooShort()
        
        val largeErrorInfo = ErrorClassifier.classifyError(tooLargeError)
        assertEquals("Recording Too Large", largeErrorInfo.title)
        assertEquals(ErrorSeverity.ERROR, largeErrorInfo.severity)
        assertTrue(largeErrorInfo.isRetryable)
        
        val shortErrorInfo = ErrorClassifier.classifyError(tooShortError)
        assertEquals("Recording Too Short", shortErrorInfo.title)
        assertEquals(ErrorSeverity.WARNING, shortErrorInfo.severity)
        assertTrue(shortErrorInfo.isRetryable)
    }
}