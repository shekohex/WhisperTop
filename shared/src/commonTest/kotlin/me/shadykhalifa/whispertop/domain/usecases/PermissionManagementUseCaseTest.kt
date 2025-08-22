package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PermissionManagementUseCaseTest {
    
    @Test
    fun `invoke should return AllGranted when repository returns GRANTED`() = runTest {
        // Given
        val repository = MockPermissionRepository(PermissionRepository.PermissionResult.GRANTED)
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        assertTrue(result.data is PermissionStatus.AllGranted)
    }
    
    @Test
    fun `invoke should return SomeDenied when repository returns DENIED`() = runTest {
        // Given
        val deniedPermissions = listOf("RECORD_AUDIO", "WRITE_EXTERNAL_STORAGE")
        val repository = MockPermissionRepository(PermissionRepository.PermissionResult.DENIED(deniedPermissions))
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val status = result.data
        assertTrue(status is PermissionStatus.SomeDenied)
        assertEquals(deniedPermissions, status.deniedPermissions)
    }
    
    @Test
    fun `invoke should return ShowRationale when repository returns SHOW_RATIONALE`() = runTest {
        // Given
        val rationalePermissions = listOf("RECORD_AUDIO")
        val repository = MockPermissionRepository(PermissionRepository.PermissionResult.SHOW_RATIONALE(rationalePermissions))
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Success)
        val status = result.data
        assertTrue(status is PermissionStatus.ShowRationale)
        assertEquals(rationalePermissions, status.permissions)
    }
    
    @Test
    fun `invoke should return Error when repository throws exception`() = runTest {
        // Given
        val exception = RuntimeException("Permission request failed")
        val repository = MockPermissionRepository(shouldThrow = true, exception = exception)
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        val result = useCase()
        
        // Then
        assertTrue(result is Result.Error)
        assertEquals(exception, result.exception)
    }
    
    @Test
    fun `checkAllPermissions should return true when repository returns ALL_GRANTED`() {
        // Given
        val repository = MockPermissionRepository(checkResult = PermissionRepository.PermissionCheckResult.ALL_GRANTED)
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        val result = useCase.checkAllPermissions()
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `checkAllPermissions should return false when repository returns SOME_DENIED`() {
        // Given
        val repository = MockPermissionRepository(checkResult = PermissionRepository.PermissionCheckResult.SOME_DENIED(listOf("RECORD_AUDIO")))
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        val result = useCase.checkAllPermissions()
        
        // Then
        assertFalse(result)
    }
    
    @Test
    fun `updatePermissionState should delegate to repository`() {
        // Given
        val repository = MockPermissionRepository()
        val useCase = PermissionManagementUseCase(repository)
        
        // When
        useCase.updatePermissionState()
        
        // Then
        assertTrue(repository.updatePermissionStateCalled)
    }
    
    private class MockPermissionRepository(
        private val requestResult: PermissionRepository.PermissionResult = PermissionRepository.PermissionResult.GRANTED,
        private val checkResult: PermissionRepository.PermissionCheckResult = PermissionRepository.PermissionCheckResult.ALL_GRANTED,
        private val shouldThrow: Boolean = false,
        private val exception: Exception = RuntimeException("Mock error")
    ) : PermissionRepository {
        
        var updatePermissionStateCalled = false
        
        override val permissionState: Flow<PermissionRepository.PermissionState> = 
            flowOf(PermissionRepository.PermissionState.GRANTED)
        
        override suspend fun requestAllPermissions(): PermissionRepository.PermissionResult {
            if (shouldThrow) throw exception
            return requestResult
        }
        
        override fun checkAllPermissions(): PermissionRepository.PermissionCheckResult = checkResult
        
        override fun updatePermissionState() {
            updatePermissionStateCalled = true
        }
    }
}