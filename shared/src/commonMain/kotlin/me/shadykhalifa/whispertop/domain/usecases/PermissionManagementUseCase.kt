package me.shadykhalifa.whispertop.domain.usecases

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.safeCall

class PermissionManagementUseCase(
    private val permissionRepository: PermissionRepository
) {
    val permissionState: Flow<Boolean> = 
        permissionRepository.permissionState.map { state ->
            state == PermissionRepository.PermissionState.GRANTED
        }
    
    suspend operator fun invoke(): Result<PermissionStatus> = safeCall {
        val result = permissionRepository.requestAllPermissions()
        
        when (result) {
            is PermissionRepository.PermissionResult.GRANTED -> {
                PermissionStatus.AllGranted
            }
            is PermissionRepository.PermissionResult.DENIED -> {
                PermissionStatus.SomeDenied(result.deniedPermissions)
            }
            is PermissionRepository.PermissionResult.SHOW_RATIONALE -> {
                PermissionStatus.ShowRationale(result.permissions)
            }
        }
    }
    
    fun checkAllPermissions(): Boolean {
        val result = permissionRepository.checkAllPermissions()
        return when (result) {
            is PermissionRepository.PermissionCheckResult.ALL_GRANTED -> true
            is PermissionRepository.PermissionCheckResult.SOME_DENIED -> false
        }
    }
    
    fun updatePermissionState() {
        permissionRepository.updatePermissionState()
    }
}