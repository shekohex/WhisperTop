package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServiceInitializationUseCaseTest {
    
    @Test
    fun `invoke should return Connected when repository returns SUCCESS`() = runTest {
        // Given
        val repository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.SUCCESS)
        val useCase = ServiceInitializationUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data is ServiceConnectionStatus.Connected)
    }
    
    @Test
    fun `invoke should return AlreadyBound when repository returns ALREADY_BOUND`() = runTest {
        // Given
        val repository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.ALREADY_BOUND)
        val useCase = ServiceInitializationUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data is ServiceConnectionStatus.AlreadyBound)
    }
    
    @Test
    fun `invoke should return Failed when repository returns FAILED`() = runTest {
        // Given
        val repository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.FAILED)
        val useCase = ServiceInitializationUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val status = result.data
        assertTrue(status is ServiceConnectionStatus.Failed)
        assertEquals("Service connection failed", status.reason)
    }
    
    @Test
    fun `invoke should return Error when repository returns ERROR`() = runTest {
        // Given
        val exception = RuntimeException("Service binding failed")
        val repository = MockServiceStateRepository(ServiceStateRepository.ServiceBindResult.ERROR(exception))
        val useCase = ServiceInitializationUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val status = result.data
        assertTrue(status is ServiceConnectionStatus.Error)
        assertEquals(exception, status.exception)
    }
    
    @Test
    fun `invoke should return Error when repository throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Unexpected error")
        val repository = MockServiceStateRepository(shouldThrow = true, exception = exception)
        val useCase = ServiceInitializationUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, result.exception)
    }
    
    private class MockServiceStateRepository(
        private val bindResult: ServiceStateRepository.ServiceBindResult = ServiceStateRepository.ServiceBindResult.SUCCESS,
        private val shouldThrow: Boolean = false,
        private val exception: Exception = RuntimeException("Mock error")
    ) : ServiceStateRepository {
        
        override val connectionState: Flow<ServiceStateRepository.ServiceConnectionState> = 
            flowOf(ServiceStateRepository.ServiceConnectionState.CONNECTED)
        override val recordingState: Flow<ServiceStateRepository.RecordingState> = 
            flowOf(ServiceStateRepository.RecordingState.IDLE)
        override val errorEvents: Flow<String> = flowOf()
        override val recordingCompleteEvents: Flow<AudioFile?> = flowOf()
        
        override suspend fun bindService(): ServiceStateRepository.ServiceBindResult {
            if (shouldThrow) throw exception
            return bindResult
        }
        
        override fun getCurrentRecordingState(): ServiceStateRepository.RecordingState = 
            ServiceStateRepository.RecordingState.IDLE
        override fun getRecordingDuration(): Long = 0L
        override fun cleanup() {}
    }
}