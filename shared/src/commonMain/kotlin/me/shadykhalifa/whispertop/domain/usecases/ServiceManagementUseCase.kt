package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.utils.Result

interface ServiceManagementUseCase {
    suspend fun bindServices(): Result<Unit>
    suspend fun checkPermissions(): Result<PermissionStatus>
    suspend fun getServiceReadiness(): Result<ServiceReadinessState>
    fun cleanup()
}

class ServiceManagementUseCaseImpl(
    private val serviceBindingUseCase: ServiceBindingUseCase,
    private val permissionManagementUseCase: PermissionManagementUseCase
) : ServiceManagementUseCase {
    
    override suspend fun bindServices(): Result<Unit> {
        return when (val result = serviceBindingUseCase()) {
            is Result.Success -> {
                if (result.data.serviceConnected) {
                    Result.Success(Unit)
                } else {
                    Result.Error(Exception(result.data.errorMessage ?: "Service binding failed"))
                }
            }
            is Result.Error -> Result.Error(result.exception)
            is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
        }
    }
    
    override suspend fun checkPermissions(): Result<PermissionStatus> {
        return permissionManagementUseCase()
    }
    
    override suspend fun getServiceReadiness(): Result<ServiceReadinessState> {
        return serviceBindingUseCase()
    }
    
    override fun cleanup() {
        // Cleanup any resources if needed
    }
}