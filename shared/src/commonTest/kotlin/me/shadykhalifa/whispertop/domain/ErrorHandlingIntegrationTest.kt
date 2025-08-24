package me.shadykhalifa.whispertop.domain

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.*
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.presentation.viewmodels.BaseViewModel
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ErrorHandlingIntegrationTest {
    
    private lateinit var errorLoggingService: ErrorLoggingService
    private lateinit var errorNotificationService: ErrorNotificationService
    private lateinit var errorMapper: ErrorMapper
    private lateinit var viewModelErrorHandler: ViewModelErrorHandler
    
    @BeforeTest
    fun setup() {
        errorLoggingService = mockk(relaxed = true)
        errorNotificationService = mockk(relaxed = true)
        errorMapper = ErrorMapperImpl(errorLoggingService)
        viewModelErrorHandler = ViewModelErrorHandler(errorMapper, errorNotificationService)
    }
    
    @Test
    fun `complete error flow should work end-to-end`() = runTest {
        // Simulate a transcription error from domain layer
        val domainError = TranscriptionError.ApiKeyMissing()
        val context = "transcription_workflow"
        
        // Process error through the complete flow
        val errorInfo = viewModelErrorHandler.handleErrorWithNotification(domainError, context)
        
        // Verify ErrorMapper correctly transformed domain error to UI-friendly ErrorInfo
        assertEquals("API Key Required", errorInfo.title)
        assertEquals("Please configure your OpenAI API key in settings to use transcription.", errorInfo.message)
        assertEquals("Open Settings", errorInfo.actionText)
        assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
        
        // Verify ErrorLoggingService captured error with proper context
        verify {
            errorLoggingService.logError(
                error = domainError,
                context = match { map ->
                    map["operation"] == context &&
                    map.containsKey("timestamp")
                },
                additionalInfo = "Mapped error from $context"
            )
        }
        
        // Verify ErrorNotificationService was notified
        verify { errorNotificationService.showError(domainError, context) }
    }
    
    @Test
    fun `BaseViewModel integration should eliminate direct error strings`() = runTest {
        // Create a test BaseViewModel implementation
        val testViewModel = object : BaseViewModel(viewModelErrorHandler) {
            fun simulateError() {
                val error = RuntimeException("Test runtime error")
                handleError(error, "test_operation")
            }
            
            fun simulateErrorWithNotification() {
                val error = TranscriptionError.NetworkError(RuntimeException("Network failed"))
                handleErrorWithNotification(error, "network_operation")
            }
        }
        
        // Test normal error handling
        testViewModel.simulateError()
        
        val errorInfo = testViewModel.errorInfo.value
        assertNotNull(errorInfo)
        assertEquals("Unexpected Error", errorInfo.title)
        assertEquals("Test runtime error", errorInfo.message)
        
        // Test error handling with notification
        testViewModel.simulateErrorWithNotification()
        
        val networkErrorInfo = testViewModel.errorInfo.value
        assertNotNull(networkErrorInfo)
        assertEquals("Connection Error", networkErrorInfo.title)
        assertEquals("Unable to connect to transcription service. Please check your internet connection.", networkErrorInfo.message)
        
        // Verify ErrorNotificationService was called
        verify { errorNotificationService.showError(any<TranscriptionError.NetworkError>(), "network_operation") }
    }
    
    @Test
    fun `error classification should be consistent across components`() = runTest {
        val errors = listOf(
            TranscriptionError.AuthenticationError("Invalid API key"),
            me.shadykhalifa.whispertop.data.audio.AudioRecordingError.PermissionDenied(),
            me.shadykhalifa.whispertop.data.models.OpenAIException.RateLimitException("Rate limit exceeded"),
            RuntimeException("Generic error")
        )
        
        errors.forEach { error ->
            val errorInfo = viewModelErrorHandler.handleError(error, "classification_test")
            
            // All errors should be properly classified with meaningful titles and messages
            assertNotNull(errorInfo.title, "Error title should not be null for ${error::class.simpleName}")
            assertNotNull(errorInfo.message, "Error message should not be null for ${error::class.simpleName}")
            
            when (error) {
                is TranscriptionError.AuthenticationError -> {
                    assertEquals("Invalid API Key", errorInfo.title)
                    assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
                }
                is me.shadykhalifa.whispertop.data.audio.AudioRecordingError.PermissionDenied -> {
                    assertEquals("Microphone Permission Required", errorInfo.title)
                    assertEquals(ErrorSeverity.CRITICAL, errorInfo.severity)
                }
                is me.shadykhalifa.whispertop.data.models.OpenAIException.RateLimitException -> {
                    assertEquals("Rate Limit Exceeded", errorInfo.title)
                    assertEquals(ErrorSeverity.WARNING, errorInfo.severity)
                }
                is RuntimeException -> {
                    assertEquals("Unexpected Error", errorInfo.title)
                    assertEquals(ErrorSeverity.ERROR, errorInfo.severity)
                }
            }
        }
    }
    
    @Test
    fun `error context should be preserved through the entire flow`() = runTest {
        val error = TranscriptionError.StorageError("Insufficient storage")
        val errorContext = ErrorContext(
            operationName = "storage_operation",
            userId = "user456",
            sessionId = "session789",
            additionalData = mapOf(
                "available_space" to "100MB",
                "required_space" to "500MB",
                "file_path" to "/temp/recording.wav"
            )
        )
        
        val errorInfo = viewModelErrorHandler.handleErrorWithContext(error, errorContext, showNotification = true)
        
        // Verify error classification
        assertEquals("Storage Error", errorInfo.title)
        assertEquals("Not enough storage space available. Please free up space and try again.", errorInfo.message)
        assertEquals(ErrorSeverity.ERROR, errorInfo.severity)
        
        // Verify context was logged with all additional data
        verify {
            errorLoggingService.logError(
                error = error,
                context = match { map ->
                    map["operation"] == "storage_operation" &&
                    map["user_id"] == "user456" &&
                    map["session_id"] == "session789" &&
                    map["available_space"] == "100MB" &&
                    map["required_space"] == "500MB" &&
                    map["file_path"] == "/temp/recording.wav" &&
                    map.containsKey("timestamp")
                },
                additionalInfo = "Mapped error from storage_operation"
            )
        }
        
        // Verify notification was sent with operation name
        verify { errorNotificationService.showError(error, "storage_operation") }
    }
    
    @Test
    fun `error recovery information should be preserved`() = runTest {
        val retryableError = TranscriptionError.RateLimitError("Rate limited")
        val nonRetryableError = TranscriptionError.ApiKeyMissing()
        
        val retryableResult = viewModelErrorHandler.handleError(retryableError)
        val nonRetryableResult = viewModelErrorHandler.handleError(nonRetryableError)
        
        // Verify retryable error properties
        assertEquals(true, retryableResult.isRetryable)
        assertEquals(10000L, retryableResult.retryDelay)
        assertEquals("Retry", retryableResult.actionText)
        
        // Verify non-retryable error properties  
        assertEquals(false, nonRetryableResult.isRetryable)
        assertEquals(0L, nonRetryableResult.retryDelay)
        assertEquals("Open Settings", nonRetryableResult.actionText)
    }
}