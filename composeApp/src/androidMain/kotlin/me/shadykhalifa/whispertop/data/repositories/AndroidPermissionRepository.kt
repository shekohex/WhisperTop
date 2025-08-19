package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.managers.PermissionHandler

class AndroidPermissionRepository(
    private val permissionHandler: PermissionHandler
) : PermissionRepository {
    
    override val permissionState: Flow<PermissionRepository.PermissionState> = 
        permissionHandler.permissionState.map { state ->
            when (state) {
                PermissionHandler.PermissionState.UNKNOWN -> 
                    PermissionRepository.PermissionState.UNKNOWN
                PermissionHandler.PermissionState.GRANTED -> 
                    PermissionRepository.PermissionState.GRANTED
                PermissionHandler.PermissionState.DENIED -> 
                    PermissionRepository.PermissionState.DENIED

            }
        }
    
    override suspend fun requestAllPermissions(): PermissionRepository.PermissionResult {
        return when (val result = permissionHandler.requestAllPermissions()) {
            is PermissionHandler.PermissionResult.GRANTED -> 
                PermissionRepository.PermissionResult.GRANTED
            is PermissionHandler.PermissionResult.DENIED -> 
                PermissionRepository.PermissionResult.DENIED(result.deniedPermissions)
            is PermissionHandler.PermissionResult.SHOW_RATIONALE -> 
                PermissionRepository.PermissionResult.SHOW_RATIONALE(result.permissions)
        }
    }
    
    override fun checkAllPermissions(): PermissionRepository.PermissionCheckResult {
        return when (val result = permissionHandler.checkAllPermissions()) {
            is PermissionHandler.PermissionCheckResult.ALL_GRANTED -> 
                PermissionRepository.PermissionCheckResult.ALL_GRANTED
            is PermissionHandler.PermissionCheckResult.SOME_DENIED -> 
                PermissionRepository.PermissionCheckResult.SOME_DENIED(result.deniedPermissions)
        }
    }
    
    override fun updatePermissionState() {
        // PermissionHandler automatically updates its state internally
        // No action needed here as the state flow will emit changes
    }
}