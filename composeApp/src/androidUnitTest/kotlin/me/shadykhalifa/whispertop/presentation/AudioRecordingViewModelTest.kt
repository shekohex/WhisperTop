package me.shadykhalifa.whispertop.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.presentation.models.RecordingStatus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import io.mockk.mockk
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioRecordingViewModelTest {
    
    private lateinit var mockTranscriptionWorkflowUseCase: TranscriptionWorkflowUseCase
    private lateinit var mockUserFeedbackUseCase: UserFeedbackUseCase
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var viewModel: AudioRecordingViewModel
    
    @Before
    fun setup() {
        mockTranscriptionWorkflowUseCase = mock()
        mockUserFeedbackUseCase = mock()
        
        // Setup default mock behavior
        whenever(mockTranscriptionWorkflowUseCase.workflowState).thenReturn(
            MutableStateFlow(WorkflowState.Idle)
        )
        
        viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            errorHandler = mockk(relaxed = true)
        )
    }
    
    @Test
    fun testViewModelCreation() {
        assertNotNull(viewModel)
    }
    
    @Test
    fun testInitialUiState() = testScope.runTest {
        val initialState = viewModel.uiState.value
        
        assertEquals(RecordingStatus.Idle, initialState.status)
        assertEquals(false, initialState.isLoading)
        assertEquals(null, initialState.errorMessage)
        assertEquals(null, initialState.lastRecording)
        assertEquals(null, initialState.transcription)
    }
    
    @Test
    fun testStartRecordingDelegatesToWorkflow() = testScope.runTest {
        viewModel.startRecording()
        
        verify(mockTranscriptionWorkflowUseCase).startRecording()
    }
    
    @Test
    fun testStopRecordingDelegatesToWorkflow() = testScope.runTest {
        viewModel.stopRecording()
        
        verify(mockTranscriptionWorkflowUseCase).stopRecording()
    }
    
    @Test
    fun testCancelRecordingDelegatesToWorkflow() {
        viewModel.cancelRecording()
        
        verify(mockTranscriptionWorkflowUseCase).cancelRecording()
    }
    
    @Test
    fun testRetryFromErrorDelegatesToWorkflow() {
        viewModel.retryFromError()
        
        verify(mockTranscriptionWorkflowUseCase).retryFromError()
    }
    
    @Test
    fun testClearError() {
        viewModel.clearError()
        
        val currentState = viewModel.uiState.value
        assertEquals(null, currentState.errorMessage)
    }
}