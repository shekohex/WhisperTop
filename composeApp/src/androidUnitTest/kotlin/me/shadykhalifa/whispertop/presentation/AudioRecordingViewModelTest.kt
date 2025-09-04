package me.shadykhalifa.whispertop.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.domain.usecases.WorkflowState
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.presentation.models.RecordingStatus
import me.shadykhalifa.whispertop.utils.Result
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AudioRecordingViewModelTest {
    
    private lateinit var mockTranscriptionWorkflowUseCase: TranscriptionWorkflowUseCase
    private lateinit var mockUserFeedbackUseCase: UserFeedbackUseCase
    private lateinit var mockServiceInitializationUseCase: ServiceInitializationUseCase
    private lateinit var mockPermissionManagementUseCase: PermissionManagementUseCase
    private lateinit var mockServiceBindingUseCase: ServiceBindingUseCase
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var viewModel: AudioRecordingViewModel
    
    @Before
    fun setup() {
        mockTranscriptionWorkflowUseCase = mock()
        mockUserFeedbackUseCase = mock()
        mockServiceInitializationUseCase = mock()
        mockPermissionManagementUseCase = mock()
        mockServiceBindingUseCase = mock()
        
        // Setup default mock behavior
        whenever(mockTranscriptionWorkflowUseCase.workflowState).thenReturn(
            MutableStateFlow(WorkflowState.Idle)
        )
        
        // Setup default service management mock behavior
        whenever(mockServiceBindingUseCase()).thenReturn(
            Result.Success(ServiceReadinessState(serviceConnected = true, permissionsGranted = true))
        )
        whenever(mockServiceInitializationUseCase()).thenReturn(
            Result.Success(ServiceConnectionStatus.Connected)
        )
        whenever(mockPermissionManagementUseCase()).thenReturn(
            Result.Success(PermissionStatus.AllGranted)
        )
        
        viewModel = AudioRecordingViewModel(
            transcriptionWorkflowUseCase = mockTranscriptionWorkflowUseCase,
            userFeedbackUseCase = mockUserFeedbackUseCase,
            serviceInitializationUseCase = ServiceInitializationUseCase(mock()),
            permissionManagementUseCase = PermissionManagementUseCase(mock()),
            serviceBindingUseCase = ServiceBindingUseCase(mock(), mock()),
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
    fun testServiceReadinessStateInitialization() = testScope.runTest {
        testScope.testScheduler.advanceUntilIdle()
        
        val serviceReadinessState = viewModel.serviceReadinessState.value
        assertNotNull(serviceReadinessState)
        assertTrue(serviceReadinessState.serviceConnected)
        assertTrue(serviceReadinessState.permissionsGranted)
        assertEquals(null, serviceReadinessState.errorMessage)
    }
    
    @Test
    fun testCheckServiceReadiness_Success() = testScope.runTest {
        val expectedReadiness = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = true
        )
        whenever(mockServiceBindingUseCase()).thenReturn(Result.Success(expectedReadiness))
        
        viewModel.checkServiceReadiness()
        testScope.testScheduler.advanceUntilIdle()
        
        val actualReadiness = viewModel.serviceReadinessState.value
        assertEquals(expectedReadiness, actualReadiness)
    }
    
    @Test
    fun testCheckServiceReadiness_Error() = testScope.runTest {
        val error = Exception("Service binding failed")
        whenever(mockServiceBindingUseCase()).thenReturn(Result.Error(error))
        
        viewModel.checkServiceReadiness()
        testScope.testScheduler.advanceUntilIdle()
        
        val readinessState = viewModel.serviceReadinessState.value
        assertNotNull(readinessState)
        assertFalse(readinessState.serviceConnected)
        assertFalse(readinessState.permissionsGranted)
        assertEquals("Service binding failed", readinessState.errorMessage)
    }
    
    @Test
    fun testInitializeServiceConnection_Success() = testScope.runTest {
        val expectedStatus = ServiceConnectionStatus.Connected
        whenever(mockServiceInitializationUseCase()).thenReturn(Result.Success(expectedStatus))
        
        val result = viewModel.initializeServiceConnection()
        testScope.testScheduler.advanceUntilIdle()
        
        assertTrue(result is Result.Success)
        assertEquals(expectedStatus, (result as Result.Success).data)
        assertEquals(expectedStatus, viewModel.serviceConnectionStatus.value)
    }
    
    @Test
    fun testInitializeServiceConnection_Error() = testScope.runTest {
        val error = Exception("Connection failed")
        whenever(mockServiceInitializationUseCase()).thenReturn(Result.Error(error))
        
        val result = viewModel.initializeServiceConnection()
        
        assertTrue(result is Result.Error)
        assertEquals(error, (result as Result.Error).exception)
    }
    
    @Test
    fun testCheckPermissions_Success() = testScope.runTest {
        val expectedStatus = PermissionStatus.AllGranted
        whenever(mockPermissionManagementUseCase()).thenReturn(Result.Success(expectedStatus))
        
        val result = viewModel.checkPermissions()
        testScope.testScheduler.advanceUntilIdle()
        
        assertTrue(result is Result.Success)
        assertEquals(expectedStatus, (result as Result.Success).data)
        assertEquals(expectedStatus, viewModel.permissionStatus.value)
    }
    
    @Test
    fun testCheckPermissions_Denied() = testScope.runTest {
        val expectedStatus = PermissionStatus.SomeDenied(listOf("RECORD_AUDIO"))
        whenever(mockPermissionManagementUseCase()).thenReturn(Result.Success(expectedStatus))
        
        val result = viewModel.checkPermissions()
        testScope.testScheduler.advanceUntilIdle()
        
        assertTrue(result is Result.Success)
        assertEquals(expectedStatus, (result as Result.Success).data)
        assertEquals(expectedStatus, viewModel.permissionStatus.value)
    }
    
    @Test
    fun testIsServiceReady_Ready() = testScope.runTest {
        viewModel.serviceReadinessState.value = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = true
        )
        
        assertTrue(viewModel.isServiceReady())
    }
    
    @Test
    fun testIsServiceReady_NotConnected() = testScope.runTest {
        viewModel.serviceReadinessState.value = ServiceReadinessState(
            serviceConnected = false,
            permissionsGranted = true
        )
        
        assertFalse(viewModel.isServiceReady())
    }
    
    @Test
    fun testIsServiceReady_NoPermissions() = testScope.runTest {
        viewModel.serviceReadinessState.value = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = false
        )
        
        assertFalse(viewModel.isServiceReady())
    }
    
    @Test
    fun testArePermissionsGranted_Granted() = testScope.runTest {
        viewModel.permissionStatus.value = PermissionStatus.AllGranted
        
        assertTrue(viewModel.arePermissionsGranted())
    }
    
    @Test
    fun testArePermissionsGranted_Denied() = testScope.runTest {
        viewModel.permissionStatus.value = PermissionStatus.SomeDenied(listOf("RECORD_AUDIO"))
        
        assertFalse(viewModel.arePermissionsGranted())
    }
    
    @Test
    fun testIsServiceConnected_Connected() = testScope.runTest {
        viewModel.serviceConnectionStatus.value = ServiceConnectionStatus.Connected
        
        assertTrue(viewModel.isServiceConnected())
    }
    
    @Test
    fun testIsServiceConnected_AlreadyBound() = testScope.runTest {
        viewModel.serviceConnectionStatus.value = ServiceConnectionStatus.AlreadyBound
        
        assertTrue(viewModel.isServiceConnected())
    }
    
    @Test
    fun testIsServiceConnection_Failed() = testScope.runTest {
        viewModel.serviceConnectionStatus.value = ServiceConnectionStatus.Failed("Connection failed")
        
        assertFalse(viewModel.isServiceConnected())
    }
    
    @Test
    fun testRefreshServiceState() = testScope.runTest {
        viewModel.refreshServiceState()
        testScope.testScheduler.advanceUntilIdle()
        
        verify(mockServiceInitializationUseCase)()
        verify(mockPermissionManagementUseCase)()
        verify(mockServiceBindingUseCase)()
    }
    
    @Test
    fun testStartRecording_WithServiceReady() = testScope.runTest {
        viewModel.serviceReadinessState.value = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = true
        )
        
        viewModel.startRecording()
        testScope.testScheduler.advanceUntilIdle()
        
        verify(mockTranscriptionWorkflowUseCase).startRecording()
    }
    
    @Test
    fun testStartRecording_WithServiceNotReady() = testScope.runTest {
        viewModel.serviceReadinessState.value = ServiceReadinessState(
            serviceConnected = false,
            permissionsGranted = false,
            errorMessage = "Services not ready"
        )
        
        // Mock service binding to still return not ready
        whenever(mockServiceBindingUseCase()).thenReturn(
            Result.Success(ServiceReadinessState(
                serviceConnected = false,
                permissionsGranted = false,
                errorMessage = "Services not ready"
            ))
        )
        
        viewModel.startRecording()
        testScope.testScheduler.advanceUntilIdle()
        
        // Should not call startRecording when services are not ready
        // Instead should refresh service state and show error feedback
        verify(mockUserFeedbackUseCase).showFeedback("Services not ready", isError = true)
    }
    
    @Test
    fun testRetryFromError_RefreshesServiceState() = testScope.runTest {
        viewModel.retryFromError()
        testScope.testScheduler.advanceUntilIdle()
        
        verify(mockServiceInitializationUseCase)()
        verify(mockPermissionManagementUseCase)()
        verify(mockServiceBindingUseCase)()
        verify(mockTranscriptionWorkflowUseCase).retryFromError()
    }
    
    @Test
    fun testStartRecordingDelegatesToWorkflow() = testScope.runTest {
        viewModel.startRecording()
        testScope.testScheduler.advanceUntilIdle()
        
        verify(mockTranscriptionWorkflowUseCase).startRecording()
    }
    
    @Test
    fun testStopRecordingDelegatesToWorkflow() = testScope.runTest {
        viewModel.stopRecording()
        testScope.testScheduler.advanceUntilIdle()
        
        verify(mockTranscriptionWorkflowUseCase).stopRecording()
    }
    
    @Test
    fun testCancelRecordingDelegatesToWorkflow() {
        viewModel.cancelRecording()
        
        verify(mockTranscriptionWorkflowUseCase).cancelRecording()
    }
    
    @Test
    fun testClearError() {
        viewModel.clearError()
        
        val currentState = viewModel.uiState.value
        assertEquals(null, currentState.errorMessage)
    }
}