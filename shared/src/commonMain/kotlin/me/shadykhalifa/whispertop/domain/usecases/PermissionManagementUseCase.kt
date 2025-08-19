package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository

class PermissionManagementUseCase(
    private val permissionRepository: PermissionRepository
) {
    val permissionState: Flow<PermissionRepository.PermissionState> = 
        permissionRepository.permissionState
    
    suspend fun requestAllPermissions(): PermissionRepository.PermissionResult {
        return permissionRepository.requestAllPermissions()
    }
    
    fun checkAllPermissions(): PermissionRepository.PermissionCheckResult {
        return permissionRepository.checkAllPermissions()
    }
    
    fun updatePermissionState() {
        permissionRepository.updatePermissionState()
    }
}