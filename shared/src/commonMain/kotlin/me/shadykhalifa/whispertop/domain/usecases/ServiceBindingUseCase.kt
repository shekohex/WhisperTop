package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceConnectionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.utils.Result

class ServiceBindingUseCase(
    private val serviceInitializationUseCase: ServiceInitializationUseCase,
    private val permissionManagementUseCase: PermissionManagementUseCase
) {
    suspend operator fun invoke(): Result<ServiceReadinessState> {
        return try {
            coroutineScope {
                val serviceDeferred = async { serviceInitializationUseCase() }
                val permissionDeferred = async { permissionManagementUseCase() }
                
                val serviceResult = serviceDeferred.await()
                val permissionResult = permissionDeferred.await()
                
                when {
                    serviceResult is Result.Error -> {
                        Result.Success(
                            ServiceReadinessState(
                                serviceConnected = false,
                                permissionsGranted = false,
                                errorMessage = "Service initialization failed"
                            )
                        )
                    }
                    permissionResult is Result.Error -> {
                        Result.Success(
                            ServiceReadinessState(
                                serviceConnected = false,
                                permissionsGranted = false,
                                errorMessage = "Permission management failed"
                            )
                        )
                    }
                    else -> {
                        val serviceStatus = (serviceResult as Result.Success).data
                        val permissionStatus = (permissionResult as Result.Success).data
                        
                        val serviceConnected = when (serviceStatus) {
                            is ServiceConnectionStatus.Connected,
                            is ServiceConnectionStatus.AlreadyBound -> true
                            else -> false
                        }
                        
                        val permissionsGranted = permissionStatus is PermissionStatus.AllGranted
                        
                        val errorMessage = when {
                            serviceStatus is ServiceConnectionStatus.Failed -> "Service connection failed"
                            serviceStatus is ServiceConnectionStatus.Error -> "Service connection error"
                            permissionStatus is PermissionStatus.SomeDenied -> 
                                "Required permissions not granted"
                            permissionStatus is PermissionStatus.ShowRationale -> 
                                "Permission rationale required"
                            else -> null
                        }
                        
                        Result.Success(
                            ServiceReadinessState(
                                serviceConnected = serviceConnected,
                                permissionsGranted = permissionsGranted,
                                errorMessage = errorMessage
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}