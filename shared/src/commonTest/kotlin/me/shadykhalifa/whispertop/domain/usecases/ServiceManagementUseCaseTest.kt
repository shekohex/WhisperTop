package me.shadykhalifa.whispertop.domain.usecases

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ServiceManagementUseCaseTest {

    private val serviceBindingUseCase = mockk<ServiceBindingUseCase>()
    private val permissionManagementUseCase = mockk<PermissionManagementUseCase>()
    
    private val serviceManagementUseCase = ServiceManagementUseCaseImpl(
        serviceBindingUseCase = serviceBindingUseCase,
        permissionManagementUseCase = permissionManagementUseCase
    )

    @Test
    fun `bindServices should return success when service binding succeeds`() = runTest {
        // Given
        val readinessState = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = true,
            errorMessage = null
        )
        coEvery { serviceBindingUseCase() } returns Result.Success(readinessState)

        // When
        val result = serviceManagementUseCase.bindServices()

        // Then
        assertIs<Result.Success<Unit>>(result)
        coVerify { serviceBindingUseCase() }
    }

    @Test
    fun `bindServices should return error when service binding fails`() = runTest {
        // Given
        val readinessState = ServiceReadinessState(
            serviceConnected = false,
            permissionsGranted = true,
            errorMessage = "Service binding failed"
        )
        coEvery { serviceBindingUseCase() } returns Result.Success(readinessState)

        // When
        val result = serviceManagementUseCase.bindServices()

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Service binding failed", result.exception.message)
        coVerify { serviceBindingUseCase() }
    }

    @Test
    fun `bindServices should return error when service binding throws exception`() = runTest {
        // Given
        val exception = Exception("Service error")
        coEvery { serviceBindingUseCase() } returns Result.Error(exception)

        // When
        val result = serviceManagementUseCase.bindServices()

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Service error", result.exception.message)
        coVerify { serviceBindingUseCase() }
    }

    @Test
    fun `checkPermissions should delegate to permissionManagementUseCase`() = runTest {
        // Given
        val permissionStatus = PermissionStatus.AllGranted
        coEvery { permissionManagementUseCase() } returns Result.Success(permissionStatus)

        // When
        val result = serviceManagementUseCase.checkPermissions()

        // Then
        assertIs<Result.Success<PermissionStatus>>(result)
        assertEquals(permissionStatus, result.data)
        coVerify { permissionManagementUseCase() }
    }

    @Test
    fun `checkPermissions should return error when permission check fails`() = runTest {
        // Given
        val exception = Exception("Permission error")
        coEvery { permissionManagementUseCase() } returns Result.Error(exception)

        // When
        val result = serviceManagementUseCase.checkPermissions()

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Permission error", result.exception.message)
        coVerify { permissionManagementUseCase() }
    }

    @Test
    fun `getServiceReadiness should delegate to serviceBindingUseCase`() = runTest {
        // Given
        val readinessState = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = false,
            errorMessage = "Permissions required"
        )
        coEvery { serviceBindingUseCase() } returns Result.Success(readinessState)

        // When
        val result = serviceManagementUseCase.getServiceReadiness()

        // Then
        assertIs<Result.Success<ServiceReadinessState>>(result)
        assertEquals(readinessState, result.data)
        coVerify { serviceBindingUseCase() }
    }

    @Test
    fun `getServiceReadiness should return error when readiness check fails`() = runTest {
        // Given
        val exception = Exception("Readiness check failed")
        coEvery { serviceBindingUseCase() } returns Result.Error(exception)

        // When
        val result = serviceManagementUseCase.getServiceReadiness()

        // Then
        assertIs<Result.Error>(result)
        assertEquals("Readiness check failed", result.exception.message)
        coVerify { serviceBindingUseCase() }
    }

    @Test
    fun `cleanup should not throw exception`() {
        // When/Then - should not throw
        serviceManagementUseCase.cleanup()
    }
}