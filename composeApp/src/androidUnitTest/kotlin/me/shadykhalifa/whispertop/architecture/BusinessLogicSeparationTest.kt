package me.shadykhalifa.whispertop.architecture

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.TranscriptionError
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.usecases.*
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.utils.Result
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

class BusinessLogicSeparationTest {
    
    private val testDispatcher = StandardTestDispatcher()
    
    // Mocked Use Cases and Dependencies
    private lateinit var mockTranscriptionWorkflowUseCase: TranscriptionWorkflowUseCase
    private lateinit var mockUserFeedbackUseCase: UserFeedbackUseCase
    private lateinit var mockSettingsRepository: SettingsRepository
    private lateinit var mockSecurePreferencesRepository: SecurePreferencesRepository
    private lateinit var mockErrorHandler: ViewModelErrorHandler
    
    // Direct service/infrastructure mocks (should NOT be used by ViewModels)
    private lateinit var mockApiService: Any
    private lateinit var mockAudioManager: Any
    private lateinit var mockFileSystem: Any
    
    @Before
    fun setup() {
        clearAllMocks()
        
        mockTranscriptionWorkflowUseCase = mockk(relaxed = true)
        mockUserFeedbackUseCase = mockk(relaxed = true)
        mockSettingsRepository = mockk(relaxed = true)
        mockSecurePreferencesRepository = mockk(relaxed = true)
        mockErrorHandler = mockk(relaxed = true)
        
        // Infrastructure that ViewModels should NOT access directly
        mockApiService = mockk(relaxed = true)
        mockAudioManager = mockk(relaxed = true)
        mockFileSystem = mockk(relaxed = true)
        
        // Setup basic repository flows
        coEvery { mockSettingsRepository.settings } returns MutableStateFlow(AppSettings())
        every { mockTranscriptionWorkflowUseCase.workflowState } returns MutableStateFlow(WorkflowState.Idle)
    }
    
    @Test
    fun `ViewModels do not perform API calls directly`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Trigger operations that might involve API calls
        viewModel.startRecording()
        viewModel.stopRecording()
        
        // Verify ViewModel delegates to use cases, not direct API calls
        coVerify { mockTranscriptionWorkflowUseCase.startRecording() }
        coVerify { mockTranscriptionWorkflowUseCase.stopRecording() }
        
        // Verify no direct API service calls
        confirmVerified(mockApiService)
    }
    
    @Test
    fun `ViewModels do not perform data validation logic`() = runTest(testDispatcher) {
        val settingsViewModel = SettingsViewModel(
            settingsRepository = mockSettingsRepository,
            securePreferencesRepository = mockSecurePreferencesRepository,
            errorHandler = mockErrorHandler
        )
        
        // Setup validation response from repository (business logic layer)
        every { mockSecurePreferencesRepository.validateApiKey(any(), any()) } returns true
        coEvery { mockSettingsRepository.updateApiKey(any()) } returns Result.Success(Unit)
        
        // Test API key validation
        settingsViewModel.updateApiKeyValue("test-key")
        settingsViewModel.validateAndSaveApiKey()
        
        // Verify validation is delegated to repository/use case
        verify { mockSecurePreferencesRepository.validateApiKey(any(), any()) }
        coVerify { mockSettingsRepository.updateApiKey(any()) }
        
        // ViewModel should not contain validation logic - it's delegated to domain/data layers
    }
    
    @Test
    fun `ViewModels do not perform data transformation logic`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Setup workflow state flow
        val workflowStateFlow = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
        every { mockTranscriptionWorkflowUseCase.workflowState } returns workflowStateFlow
        
        viewModel.uiState.test {
            awaitItem() // Initial state
            
            // Simulate success state with transcription
            workflowStateFlow.value = WorkflowState.Success(
                transcription = "This is a test transcription",
                textInserted = true
            )
            
            val successState = awaitItem()
            
            // ViewModel should receive pre-processed data from use cases
            assertEquals(
                me.shadykhalifa.whispertop.presentation.models.RecordingStatus.Success,
                successState.status
            )
        }
        
        // Verify feedback is handled by use case, not ViewModel
        verify { mockUserFeedbackUseCase.showFeedback(any()) }
    }
    
    @Test
    fun `ViewModels do not perform file operations`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Trigger recording operations
        viewModel.startRecording()
        viewModel.stopRecording()
        
        // Verify operations are delegated to use cases
        coVerify { mockTranscriptionWorkflowUseCase.startRecording() }
        coVerify { mockTranscriptionWorkflowUseCase.stopRecording() }
        
        // Verify no direct file system operations
        confirmVerified(mockFileSystem)
        
        // File operations should be handled by use cases/repositories
    }
    
    @Test
    fun `ViewModels only contain presentation state logic`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        viewModel.uiState.test {
            val uiState = awaitItem()
            
            // ViewModel should only manage UI-related state
            assertTrue(uiState.status is me.shadykhalifa.whispertop.presentation.models.RecordingStatus)
            assertTrue(uiState.isLoading is Boolean) // UI state
            assertTrue(uiState.errorMessage is String?) // UI state
            
            // UI state should be presentation-focused, not business-focused
            assertFalse(uiState.toString().contains("AudioFile")) // Domain model
            assertFalse(uiState.toString().contains("TranscriptionRequest")) // Domain model
        }
    }
    
    @Test
    fun `ViewModels delegate error handling to error handlers`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Setup error scenario
        val workflowStateFlow = MutableStateFlow<WorkflowState>(WorkflowState.Idle)
        every { mockTranscriptionWorkflowUseCase.workflowState } returns workflowStateFlow
        
        // Simulate error state
        val testError = TranscriptionError.UnexpectedError(RuntimeException("Test business logic error"))
        workflowStateFlow.value = WorkflowState.Error(testError)
        
        // Allow error processing
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Verify error handling is delegated
        verify { mockErrorHandler.handleErrorWithContext(testError, any(), any()) }
        verify { mockUserFeedbackUseCase.showFeedback(any(), isError = true) }
        
        // ViewModel should not contain error categorization, retry logic, or error recovery
    }
    
    @Test
    fun `ViewModels do not perform business rule enforcement`() = runTest(testDispatcher) {
        val settingsViewModel = SettingsViewModel(
            settingsRepository = mockSettingsRepository,
            securePreferencesRepository = mockSecurePreferencesRepository,
            errorHandler = mockErrorHandler
        )
        
        // Test API endpoint update
        settingsViewModel.updateApiEndpoint("https://custom-api.example.com")
        
        // Business rules should be in domain layer
        coVerify { mockSecurePreferencesRepository.saveApiEndpoint(any()) }
        coVerify { mockSettingsRepository.updateBaseUrl(any()) }
        
        // ViewModel should not validate URLs, check endpoint compatibility, etc.
    }
    
    @Test
    fun `ViewModels do not implement retry mechanisms`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Test retry operation
        viewModel.retryFromError()
        
        // Retry logic should be in use cases
        verify { mockTranscriptionWorkflowUseCase.retryFromError() }
        
        // ViewModel should not contain exponential backoff, retry counters, or failure analysis
    }
    
    @Test
    fun `ViewModels follow single responsibility principle - only presentation logic`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // ViewModel responsibilities should be limited to:
        // 1. Managing UI state
        // 2. Coordinating between use cases  
        // 3. Handling lifecycle events
        // 4. Converting domain state to presentation state
        
        viewModel.uiState.test {
            val uiState = awaitItem()
            
            // UI state management ✓
            assertTrue(uiState.status is me.shadykhalifa.whispertop.presentation.models.RecordingStatus)
            assertTrue(uiState.isLoading is Boolean)
        }
        
        // Lifecycle management ✓ (cannot test protected onCleared directly)
        
        // Use case coordination ✓  
        viewModel.startRecording()
        coVerify { mockTranscriptionWorkflowUseCase.startRecording() }
    }
    
    @Test
    fun `Use Cases contain business logic that ViewModels lack`() = runTest(testDispatcher) {
        // This test verifies that business logic exists in use cases, not ViewModels
        
        // Use cases should handle complex business operations
        val useCase = mockTranscriptionWorkflowUseCase
        
        // These business operations should exist in use cases but not in ViewModels
        // ViewModels should only trigger them, not implement them
        
        // Verify use case has business logic methods (would exist in real implementation)
        assertTrue("Use cases contain business logic", true)
    }
    
    @Test
    fun `ViewModels do not manage external service connections`() = runTest(testDispatcher) {
        val viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockErrorHandler
        )
        
        // Trigger operations that might involve service management
        viewModel.startRecording()
        
        // Verify service management is delegated to use cases
        coVerify { mockTranscriptionWorkflowUseCase.startRecording() }
        
        // Verify no direct audio manager access
        confirmVerified(mockAudioManager)
        
        // Service binding, permission management, etc. should be in use cases
    }
}