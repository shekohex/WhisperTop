package me.shadykhalifa.whispertop.data.usecases

import android.util.Log
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase

/**
 * Android implementation of ServiceBindingUseCase.
 * Coordinates service initialization and permission requests to determine overall readiness.
 */
class ServiceBindingUseCaseImpl(
    private val serviceInitializationUseCase: ServiceInitializationUseCase,
    private val permissionManagementUseCase: PermissionManagementUseCase
) : ServiceBindingUseCase {
    
    private companion object {
        const val TAG = "ServiceBindingUseCase"
    }
    
    override suspend fun invoke(): Result<ServiceReadinessState> {
        return try {
            Log.d(TAG, "Starting service binding coordination")
            
            // Run service initialization and permission requests in parallel
            val readinessState = coroutineScope {
                val serviceDeferred = async { serviceInitializationUseCase() }
                val permissionDeferred = async { permissionManagementUseCase() }
                
                val serviceResult = serviceDeferred.await()
                val permissionResult = permissionDeferred.await()
                
                buildReadinessState(serviceResult, permissionResult)
            }
            
            Log.d(TAG, "Service binding coordination completed: isReady=${readinessState.isReady}")
            Result.success(readinessState)
        } catch (exception: Exception) {
            Log.e(TAG, "Service binding coordination failed with exception", exception)
            val errorState = ServiceReadinessState.notReady(
                serviceConnectionStatus = ServiceConnectionStatus.Error(
                    message = "Coordination failed: ${exception.message}",
                    cause = exception
                ),
                permissionStatus = PermissionStatus.Denied(listOf("Unknown")),
                errorMessage = "Service binding coordination failed: ${exception.message}"
            )
            Result.success(errorState)
        }
    }
    
    /**
     * Builds the service readiness state from service and permission results.
     */
    private fun buildReadinessState(
        serviceResult: Result<ServiceConnectionStatus>,
        permissionResult: Result<PermissionStatus>
    ): ServiceReadinessState {
        val serviceStatus = serviceResult.getOrElse { throwable ->
            Log.e(TAG, "Service initialization failed", throwable)
            ServiceConnectionStatus.Error(
                message = "Service initialization failed: ${throwable.message}",
                cause = throwable
            )
        }
        
        val permissionStatus = permissionResult.getOrElse { throwable ->
            Log.e(TAG, "Permission management failed", throwable)
            PermissionStatus.Denied(listOf("Permission request failed: ${throwable.message}"))
        }
        
        val errorMessage = buildErrorMessage(serviceStatus, permissionStatus)
        
        return if (errorMessage != null) {
            ServiceReadinessState.notReady(
                serviceConnectionStatus = serviceStatus,
                permissionStatus = permissionStatus,
                errorMessage = errorMessage
            )
        } else {
            ServiceReadinessState.ready(
                serviceConnectionStatus = serviceStatus,
                permissionStatus = permissionStatus
            )
        }
    }
    
    /**
     * Builds an appropriate error message based on service and permission status.
     */
    private fun buildErrorMessage(
        serviceStatus: ServiceConnectionStatus,
        permissionStatus: PermissionStatus
    ): String? {
        val serviceError = when (serviceStatus) {
            is ServiceConnectionStatus.Failed -> "Service binding failed"
            is ServiceConnectionStatus.Error -> "Service error: ${serviceStatus.message}"
            else -> null
        }
        
        val permissionError = when (permissionStatus) {
            is PermissionStatus.Denied -> 
                "Required permissions denied: ${permissionStatus.deniedPermissions.joinToString()}"
            is PermissionStatus.RequiresRationale -> 
                "Permission rationale required for: ${permissionStatus.permissions.joinToString()}"
            else -> null
        }
        
        return when {
            serviceError != null && permissionError != null -> "$serviceError; $permissionError"
            serviceError != null -> serviceError
            permissionError != null -> permissionError
            else -> null
        }
    }
}