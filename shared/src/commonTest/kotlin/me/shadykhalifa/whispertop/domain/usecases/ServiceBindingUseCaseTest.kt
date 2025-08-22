package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServiceBindingUseCaseTest {
    
    @Test
    fun `invoke should return ready state when service connected and permissions granted`() = runTest {
        // Given
        val serviceRepository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.SUCCESS)
        val permissionRepository = MockPermissionRepository(PermissionRepository.PermissionResult.GRANTED)
        val serviceUseCase = ServiceInitializationUseCase(serviceRepository)
        val permissionUseCase = PermissionManagementUseCase(permissionRepository)
        val useCase = ServiceBindingUseCase(serviceUseCase, permissionUseCase)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val state = result.data
        assertTrue(state.serviceConnected)
        assertTrue(state.permissionsGranted)
        assertNull(state.errorMessage)
        assertTrue(state.isReady)
    }
    
    @Test
    fun `invoke should return ready state when service already bound and permissions granted`() = runTest {
        // Given
        val serviceRepository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.ALREADY_BOUND)
        val permissionRepository = MockPermissionRepository(PermissionRepository.PermissionResult.GRANTED)
        val serviceUseCase = ServiceInitializationUseCase(serviceRepository)
        val permissionUseCase = PermissionManagementUseCase(permissionRepository)
        val useCase = ServiceBindingUseCase(serviceUseCase, permissionUseCase)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val state = result.data
        assertTrue(state.serviceConnected)
        assertTrue(state.permissionsGranted)
        assertNull(state.errorMessage)
        assertTrue(state.isReady)
    }
    
    @Test
    fun `invoke should return not ready when service failed`() = runTest {
        // Given
        val serviceRepository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.FAILED)
        val permissionRepository = MockPermissionRepository(PermissionRepository.PermissionResult.GRANTED)
        val serviceUseCase = ServiceInitializationUseCase(serviceRepository)
        val permissionUseCase = PermissionManagementUseCase(permissionRepository)
        val useCase = ServiceBindingUseCase(serviceUseCase, permissionUseCase)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val state = result.data
        assertFalse(state.serviceConnected)
        assertTrue(state.permissionsGranted)
        assertEquals("Service connection failed", state.errorMessage)
        assertFalse(state.isReady)
    }
    
    @Test
    fun `invoke should return not ready when service has error`() = runTest {
        // Given
        val exception = RuntimeException("Service error")
        val serviceRepository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.ERROR(exception))
        val permissionRepository = MockPermissionRepository(PermissionRepository.PermissionResult.GRANTED)
        val serviceUseCase = ServiceInitializationUseCase(serviceRepository)
        val permissionUseCase = PermissionManagementUseCase(permissionRepository)
        val useCase = ServiceBindingUseCase(serviceUseCase, permissionUseCase)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val state = result.data
        assertFalse(state.serviceConnected)
        assertTrue(state.permissionsGranted)
        assertEquals("Service connection error", state.errorMessage)
        assertFalse(state.isReady)
    }
    
    @Test
    fun `invoke should return not ready when permissions denied`() = runTest {
        // Given
        val serviceRepository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.SUCCESS)
        val permissionRepository = MockPermissionRepository(PermissionRepository.PermissionResult.DENIED(listOf("RECORD_AUDIO")))
        val serviceUseCase = ServiceInitializationUseCase(serviceRepository)
        val permissionUseCase = PermissionManagementUseCase(permissionRepository)
        val useCase = ServiceBindingUseCase(serviceUseCase, permissionUseCase)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val state = result.data
        assertTrue(state.serviceConnected)
        assertFalse(state.permissionsGranted)
        assertEquals("Required permissions not granted", state.errorMessage)
        assertFalse(state.isReady)
    }
    
    private class MockServiceStateRepository(
        private val bindResult: ServiceStateRepository.ServiceBindResult = ServiceStateRepository.ServiceBindResult.SUCCESS
    ) : ServiceStateRepository {
        
        override val connectionState: Flow<ServiceStateRepository.ServiceConnectionState> = 
            flowOf(ServiceStateRepository.ServiceConnectionState.CONNECTED)
        override val recordingState: Flow<ServiceStateRepository.RecordingState> = 
            flowOf(ServiceStateRepository.RecordingState.IDLE)
        override val errorEvents: Flow<String> = flowOf()
        override val recordingCompleteEvents: Flow<AudioFile?> = flowOf()
        
        override suspend fun bindService(): ServiceStateRepository.ServiceBindResult = bindResult
        override fun getCurrentRecordingState(): ServiceStateRepository.RecordingState = 
            ServiceStateRepository.RecordingState.IDLE
        override fun getRecordingDuration(): Long = 0L
        override fun cleanup() {}
    }
    
    private class MockPermissionRepository(
        private val requestResult: PermissionRepository.PermissionResult = PermissionRepository.PermissionResult.GRANTED,
        private val checkResult: PermissionRepository.PermissionCheckResult = PermissionRepository.PermissionCheckResult.ALL_GRANTED
    ) : PermissionRepository {
        
        override val permissionState: Flow<PermissionRepository.PermissionState> = 
            flowOf(PermissionRepository.PermissionState.GRANTED)
        
        override suspend fun requestAllPermissions(): PermissionRepository.PermissionResult = requestResult
        override fun checkAllPermissions(): PermissionRepository.PermissionCheckResult = checkResult
        override fun updatePermissionState() {}
    }
}