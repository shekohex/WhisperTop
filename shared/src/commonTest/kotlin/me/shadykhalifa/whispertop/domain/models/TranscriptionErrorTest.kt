package me.shadykhalifa.whispertop.domain.models

import kotlin.test.*

class TranscriptionErrorTest {
    
    @Test
    fun `fromThrowable should return TranscriptionError when given TranscriptionError`() {
        // Given
        val originalError = TranscriptionError.ApiKeyMissing()
        
        // When
        val result = TranscriptionError.fromThrowable(originalError)
        
        // Then
        assertSame(originalError, result)
    }
    
    @Test
    fun `fromThrowable should return ApiKeyMissing for API key IllegalStateException`() {
        // Given
        val exception = IllegalStateException("API key not configured")
        
        // When
        val result = TranscriptionError.fromThrowable(exception)
        
        // Then
        assertTrue(result is TranscriptionError.ApiKeyMissing)
    }
    
    @Test
    fun `fromThrowable should return RecordingInProgress for already recording exception`() {
        // Given
        val exception = IllegalStateException("Already recording")
        
        // When
        val result = TranscriptionError.fromThrowable(exception)
        
        // Then
        assertTrue(result is TranscriptionError.RecordingInProgress)
    }
    
    @Test
    fun `fromThrowable should return AccessibilityServiceNotEnabled for accessibility exception`() {
        // Given
        val exception = IllegalStateException("accessibility service not enabled")
        
        // When
        val result = TranscriptionError.fromThrowable(exception)
        
        // Then
        assertTrue(result is TranscriptionError.AccessibilityServiceNotEnabled)
    }
    
    @Test
    fun `fromThrowable should return UnexpectedError for other exceptions`() {
        // Given
        val exception = RuntimeException("Unknown error")
        
        // When
        val result = TranscriptionError.fromThrowable(exception)
        
        // Then
        assertTrue(result is TranscriptionError.UnexpectedError)
        assertEquals("Unknown error", result.message)
    }
    
    @Test
    fun `error messages should be user friendly`() {
        // Given & When & Then
        assertEquals(
            "API key not configured. Please set your OpenAI API key in settings.",
            TranscriptionError.ApiKeyMissing().message
        )
        
        assertEquals(
            "Text insertion requires accessibility service. Please enable WhisperTop accessibility service in system settings.",
            TranscriptionError.AccessibilityServiceNotEnabled().message
        )
        
        assertEquals(
            "Invalid API key. Please check your OpenAI API key.",
            TranscriptionError.AuthenticationError().message
        )
        
        assertEquals(
            "API rate limit exceeded. Please try again later.",
            TranscriptionError.RateLimitError().message
        )
    }
}