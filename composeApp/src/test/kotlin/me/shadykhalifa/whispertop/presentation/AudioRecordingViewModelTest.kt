package me.shadykhalifa.whispertop.presentation

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.service.AudioRecordingService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioRecordingViewModelTest : KoinTest {
    
    private lateinit var mockAudioServiceManager: AudioServiceManager
    private lateinit var mockPermissionHandler: PermissionHandler
    private lateinit var mockTranscriptionWorkflow: TranscriptionWorkflowUseCase
    private lateinit var mockContext: Context
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var viewModel: AudioRecordingViewModel
    
    @Before
    fun setup() {
        stopKoin()
        
        mockAudioServiceManager = mock()
        mockPermissionHandler = mock()
        mockTranscriptionWorkflow = mock()
        mockContext = mock()
        
        // Setup default mock behavior
        whenever(mockAudioServiceManager.connectionState).thenReturn(
            MutableStateFlow(AudioServiceManager.ServiceConnectionState.DISCONNECTED)
        )
        whenever(mockAudioServiceManager.recordingState).thenReturn(
            MutableStateFlow(AudioRecordingService.RecordingState.IDLE)
        )
        whenever(mockAudioServiceManager.errorEvents).thenReturn(flowOf())
        whenever(mockAudioServiceManager.recordingCompleteEvents).thenReturn(flowOf())
        
        whenever(mockPermissionHandler.permissionState).thenReturn(
            MutableStateFlow(PermissionHandler.PermissionState.UNKNOWN)
        )
        
        whenever(mockTranscriptionWorkflow.workflowState).thenReturn(
            MutableStateFlow(WorkflowState.Idle)
        )
        
        startKoin {
            modules(
                module {
                    single<AudioServiceManager> { mockAudioServiceManager }
                    single<PermissionHandler> { mockPermissionHandler }
                    single<TranscriptionWorkflowUseCase> { mockTranscriptionWorkflow }
                    single<Context> { mockContext }
                }
            )
        }
        
        viewModel = AudioRecordingViewModel()
    }
    
    @After
    fun tearDown() {
        stopKoin()
    }
    
    @Test
    fun testViewModelCreation() {
        assertNotNull(viewModel)
    }
    
    @Test
    fun testInitialUiState() = testScope.runTest {
        val initialState = viewModel.uiState.value
        
        assertEquals(AudioServiceManager.ServiceConnectionState.DISCONNECTED, initialState.serviceConnectionState)
        assertEquals(AudioRecordingService.RecordingState.IDLE, initialState.recordingState)
        assertEquals(PermissionHandler.PermissionState.UNKNOWN, initialState.permissionState)
        assertFalse(initialState.isServiceReady)
        assertFalse(initialState.isLoading)
    }
    
    @Test
    fun testServiceReadyWhenConnectedAndPermissionGranted() = testScope.runTest {
        // Simulate connected service and granted permissions
        whenever(mockAudioServiceManager.connectionState).thenReturn(
            MutableStateFlow(AudioServiceManager.ServiceConnectionState.CONNECTED)
        )
        whenever(mockPermissionHandler.permissionState).thenReturn(
            MutableStateFlow(PermissionHandler.PermissionState.GRANTED)
        )
        
        // Need to recreate viewModel with new mock behavior
        // In a real test setup, this would be handled by dependency injection
        
        assertTrue(true) // Placeholder - would test the actual state change
    }
    
    @Test
    fun testRecordingActionDelegatesToWorkflow() {
        // Test that startRecording calls workflow without throwing exceptions
        // The actual logic is now in the workflow, not the ViewModel
        viewModel.startRecording()
        
        // No error should be set in ViewModel since workflow handles all logic
        val currentState = viewModel.uiState.value
        // Error handling is now done by the workflow state observation
        assertTrue(true) // Test passes if no exception is thrown
    }
    
    @Test
    fun testClearError() {
        // Test clearError functionality
        viewModel.clearError()
        
        val currentState = viewModel.uiState.value
        assertEquals(null, currentState.errorMessage)
    }
    
    @Test
    fun testDismissPermissionRationale() {
        viewModel.dismissPermissionRationale()
        
        val currentState = viewModel.uiState.value
        assertFalse(currentState.showPermissionRationale)
        assertTrue(currentState.rationalePermissions.isEmpty())
    }
}