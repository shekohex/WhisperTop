package me.shadykhalifa.whispertop.presentation.utils

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class ViewModelErrorHandlerTest {
    
    private lateinit var errorMapper: ErrorMapper
    private lateinit var errorNotificationService: ErrorNotificationService
    private lateinit var viewModelErrorHandler: ViewModelErrorHandler
    
    private lateinit var mockErrorInfo: ErrorInfo
    
    @BeforeTest
    fun setup() {
        errorMapper = mockk()
        errorNotificationService = mockk(relaxed = true)
        viewModelErrorHandler = ViewModelErrorHandler(errorMapper, errorNotificationService)
        
        mockErrorInfo = ErrorInfo(
            title = "Test Error",
            message = "Test error message",
            actionText = "Retry",
            isRetryable = true,
            severity = ErrorSeverity.ERROR
        )
    }
    
    @Test
    fun `handleError should delegate to ErrorMapper and return ErrorInfo`() = runTest {
        val error = RuntimeException("Test error")
        val context = "test_context"
        
        every { errorMapper.mapToErrorInfo(error, context) } returns mockErrorInfo
        
        val result = viewModelErrorHandler.handleError(error, context, showNotification = false)
        
        assertSame(mockErrorInfo, result)
        verify { errorMapper.mapToErrorInfo(error, context) }
        verify(exactly = 0) { errorNotificationService.showError(any(), any()) }
    }
    
    @Test
    fun `handleError should show notification when requested`() = runTest {
        val error = RuntimeException("Test error")
        val context = "test_context"
        
        every { errorMapper.mapToErrorInfo(error, context) } returns mockErrorInfo
        
        val result = viewModelErrorHandler.handleError(error, context, showNotification = true)
        
        assertSame(mockErrorInfo, result)
        verify { errorMapper.mapToErrorInfo(error, context) }
        verify { errorNotificationService.showError(error, context) }
    }
    
    @Test
    fun `handleErrorWithContext should delegate to ErrorMapper with context`() = runTest {
        val error = RuntimeException("Test error")
        val errorContext = ErrorContext(
            operationName = "test_operation",
            userId = "user123",
            sessionId = "session456"
        )
        
        every { errorMapper.mapToErrorInfoWithContext(error, errorContext) } returns mockErrorInfo
        
        val result = viewModelErrorHandler.handleErrorWithContext(error, errorContext, showNotification = false)
        
        assertSame(mockErrorInfo, result)
        verify { errorMapper.mapToErrorInfoWithContext(error, errorContext) }
        verify(exactly = 0) { errorNotificationService.showError(any(), any()) }
    }
    
    @Test
    fun `handleErrorWithContext should show notification with operation name`() = runTest {
        val error = RuntimeException("Test error")
        val errorContext = ErrorContext(
            operationName = "test_operation",
            userId = "user123"
        )
        
        every { errorMapper.mapToErrorInfoWithContext(error, errorContext) } returns mockErrorInfo
        
        val result = viewModelErrorHandler.handleErrorWithContext(error, errorContext, showNotification = true)
        
        assertSame(mockErrorInfo, result)
        verify { errorMapper.mapToErrorInfoWithContext(error, errorContext) }
        verify { errorNotificationService.showError(error, "test_operation") }
    }
    
    @Test
    fun `handleErrorWithNotification should show notification by default`() = runTest {
        val error = RuntimeException("Test error")
        val context = "notification_test"
        
        every { errorMapper.mapToErrorInfo(error, context) } returns mockErrorInfo
        
        val result = viewModelErrorHandler.handleErrorWithNotification(error, context)
        
        assertSame(mockErrorInfo, result)
        verify { errorMapper.mapToErrorInfo(error, context) }
        verify { errorNotificationService.showError(error, context) }
    }
    
    @Test
    fun `handleError should handle null context`() = runTest {
        val error = RuntimeException("Test error")
        
        every { errorMapper.mapToErrorInfo(error, null) } returns mockErrorInfo
        
        val result = viewModelErrorHandler.handleError(error, context = null, showNotification = false)
        
        assertSame(mockErrorInfo, result)
        verify { errorMapper.mapToErrorInfo(error, null) }
    }
    
    @Test
    fun `should maintain error classification from ErrorMapper`() = runTest {
        val error = TranscriptionError.ApiKeyMissing()
        val context = "api_key_validation"
        
        val expectedErrorInfo = ErrorInfo(
            title = "API Key Required",
            message = "Please configure your OpenAI API key in settings to use transcription.",
            actionText = "Open Settings",
            severity = ErrorSeverity.CRITICAL
        )
        
        every { errorMapper.mapToErrorInfo(error, context) } returns expectedErrorInfo
        
        val result = viewModelErrorHandler.handleError(error, context)
        
        assertEquals("API Key Required", result.title)
        assertEquals("Please configure your OpenAI API key in settings to use transcription.", result.message)
        assertEquals("Open Settings", result.actionText)
        assertEquals(ErrorSeverity.CRITICAL, result.severity)
    }
}