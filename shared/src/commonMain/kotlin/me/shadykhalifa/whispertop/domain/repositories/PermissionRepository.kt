package me.shadykhalifa.whispertop.domain.repositories

import kotlinx.coroutines.flow.Flow

interface PermissionRepository {
    val permissionState: Flow<PermissionState>
    
    suspend fun requestAllPermissions(): PermissionResult
    fun checkAllPermissions(): PermissionCheckResult
    fun updatePermissionState()
    
    sealed class PermissionState {
        object UNKNOWN : PermissionState()
        object GRANTED : PermissionState()
        object DENIED : PermissionState()
    }
    
    sealed class PermissionResult {
        object GRANTED : PermissionResult()
        data class DENIED(val deniedPermissions: List<String>) : PermissionResult()
        data class SHOW_RATIONALE(val permissions: List<String>) : PermissionResult()
    }
    
    sealed class PermissionCheckResult {
        object ALL_GRANTED : PermissionCheckResult()
        data class SOME_DENIED(val deniedPermissions: List<String>) : PermissionCheckResult()
    }
}