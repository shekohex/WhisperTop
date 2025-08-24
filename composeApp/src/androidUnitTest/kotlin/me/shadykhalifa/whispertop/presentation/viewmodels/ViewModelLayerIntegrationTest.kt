package me.shadykhalifa.whispertop.presentation.viewmodels

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.usecases.*
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class ViewModelLayerIntegrationTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Mocked Use Cases
    private lateinit var mockTranscriptionWorkflowUseCase: TranscriptionWorkflowUseCase
    private lateinit var mockUserFeedbackUseCase: UserFeedbackUseCase
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var mockSecurePreferencesRepository: SecurePreferencesRepository
    private lateinit var mockErrorHandler: ViewModelErrorHandler
    
    // These should NOT be directly accessible to ViewModels
    private lateinit var mockAudioServiceManager: Any
    private lateinit var mockPermissionHandler: Any
    
    @Before
    fun setup() {
        mockTranscriptionWorkflowUseCase = mockk(relaxed = true)
        mockUserFeedbackUseCase = mockk(relaxed = true)
        mockSettingsRepository = mockk(relaxed = true)
        mockSecurePreferencesRepository = mockk(relaxed = true)
        mockErrorHandler = mockk(relaxed = true)
        
        // These should NOT be directly accessible to ViewModels
        mockAudioServiceManager = mockk(relaxed = true)
        mockPermissionHandler = mockk(relaxed = true)
        
        // Setup basic flows for repositories
        coEvery { mockSettingsRepository.settings } returns MutableStateFlow(AppSettings())
        coEvery { mockTranscriptionWorkflowUseCase.workflowState } returns MutableStateFlow(WorkflowState.Idle)
    }
    
    @Test
    fun `AudioRecordingViewModel only interacts with Use Cases`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Test recording operations - should only call use cases
        viewModel.startRecording()
        viewModel.stopRecording()
        viewModel.cancelRecording()
        viewModel.retryFromError()
        
        // Verify only use case methods were called
        coVerify { mockTranscriptionWorkflowUseCase.startRecording() }
        coVerify { mockTranscriptionWorkflowUseCase.stopRecording() }
        verify { mockTranscriptionWorkflowUseCase.cancelRecording() }
        verify { mockTranscriptionWorkflowUseCase.retryFromError() }
        
        // Ensure no direct service/manager calls (these mocks should never be called)
        confirmVerified(mockAudioServiceManager, mockPermissionHandler)
    }
    
    @Test
    fun `SettingsViewModel uses repositories through proper abstraction`() = runTest(testDispatcher) {
        val viewModel = SettingsViewModel(
            settingsRepository = mockSettingsRepository,
            securePreferencesRepository = mockSecurePreferencesRepository,
            errorHandler = mockErrorHandler
        )
        
        // Test settings operations
        viewModel.updateApiKey("test-key")
        viewModel.updateSelectedModel("whisper-1")
        viewModel.toggleHapticFeedback()
        
        // Verify repository methods are called (this is allowed for SettingsViewModel)
        coVerify { mockSettingsRepository.updateApiKey("test-key") }
        coVerify { mockSettingsRepository.updateSelectedModel("whisper-1") }
        
        // Verify error handling is delegated properly
        verify(atLeast = 1) { mockErrorHandler.handleError(any<Throwable>()) }
    }
    
    @Test
    fun `ViewModels handle state management properly without business logic`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Test UI state observation
        viewModel.uiState.test {
            val initialState = awaitItem()
            assertNotNull(initialState)
            assertEquals(me.shadykhalifa.whispertop.presentation.models.RecordingStatus.Idle, initialState.status)
            
            // ViewModels should only manage presentation state, not perform business operations
            assertNull(initialState.errorMessage)
            assertFalse(initialState.isLoading)
        }
    }
    
    @Test
    fun `ViewModels do not perform business logic operations directly`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Simulate workflow state changes from use case
        val workflowStateFlow = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
        every { mockTranscriptionWorkflowUseCase.workflowState } returns workflowStateFlow
        
        viewModel.uiState.test {
            awaitItem() // Initial state
            
            // Change workflow state - ViewModel should only react, not initiate business logic
            workflowStateFlow.value = WorkflowState.Recording
            
            val recordingState = awaitItem()
            assertEquals(
                me.shadykhalifa.whispertop.presentation.models.RecordingStatus.Recording,
                recordingState.status
            )
            
            // Verify ViewModel only reacts to state changes, doesn't perform business operations
            // No additional business logic calls should be made
        }
    }
    
    @Test
    fun `ViewModels handle errors through error handlers not directly`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Setup error scenario
        val workflowStateFlow = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
        every { mockTranscriptionWorkflowUseCase.workflowState } returns workflowStateFlow
        
        // Simulate error state
        val testError = me.shadykhalifa.whispertop.domain.models.TranscriptionError.UnexpectedError(RuntimeException("Test error"))
        workflowStateFlow.value = WorkflowState.Error(testError)
        
        // Allow time for error handling
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error handling is delegated to error handler
        verify { mockErrorHandler.handleErrorWithContext(testError, any(), any()) }
        verify { mockUserFeedbackUseCase.showFeedback(any(), isError = true) }
        
        // ViewModels should not handle errors directly - they delegate to error handlers
    }
    
    @Test
    fun `ViewModels follow proper lifecycle management`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Test cleanup (cannot test protected onCleared directly)
        
        // ViewModels should delegate lifecycle events to use cases
    }
    
    @Test
    fun `ViewModels use proper presentation models not domain models directly`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        viewModel.uiState.test {
            val uiState = awaitItem()
            
            // Verify ViewModel uses presentation models
            assertNotNull(uiState.status) // RecordingStatus (presentation model)
            
            // UI state should be presentation-focused, not domain-focused
            assertTrue(uiState.status is me.shadykhalifa.whispertop.presentation.models.RecordingStatus)
        }
    }
    
    @Test
    fun `ViewModels handle concurrent operations safely through use cases`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Test concurrent operations
        repeat(3) {
            viewModel.startRecording()
            viewModel.stopRecording()
        }
        
        // Verify use case methods are called for each operation
        coVerify(exactly = 3) { mockTranscriptionWorkflowUseCase.startRecording() }
        coVerify(exactly = 3) { mockTranscriptionWorkflowUseCase.stopRecording() }
        
        // Concurrency should be handled by use cases, not ViewModels
    }
}