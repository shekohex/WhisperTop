package me.shadykhalifa.whispertop.data.usecases

import android.util.Log
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.managers.PermissionHandler

/**
 * Android implementation of PermissionManagementUseCase.
 * Maps PermissionHandler.PermissionResult to domain PermissionStatus.
 */
class PermissionManagementUseCaseImpl(
    private val permissionHandler: PermissionHandler
) : PermissionManagementUseCase {
    
    private companion object {
        const val TAG = "PermissionManagementUseCase"
    }
    
    override suspend fun invoke(): Result<PermissionStatus> {
        return try {
            Log.d(TAG, "Starting permission request")
            
            val result = permissionHandler.requestAllPermissions()
            val permissionStatus = mapToPermissionStatus(result)
            
            Log.d(TAG, "Permission request completed: $permissionStatus")
            Result.success(permissionStatus)
        } catch (exception: Exception) {
            Log.e(TAG, "Permission request failed with exception", exception)
            val errorStatus = PermissionStatus.Denied(
                deniedPermissions = listOf("Unknown - exception occurred: ${exception.message}")
            )
            Result.success(errorStatus)
        }
    }
    
    /**
     * Maps infrastructure PermissionResult to domain PermissionStatus.
     */
    private fun mapToPermissionStatus(result: PermissionHandler.PermissionResult): PermissionStatus {
        return when (result) {
            is PermissionHandler.PermissionResult.GRANTED -> {
                Log.d(TAG, "All permissions granted")
                PermissionStatus.Granted
            }
            is PermissionHandler.PermissionResult.DENIED -> {
                Log.w(TAG, "Permissions denied: ${result.deniedPermissions}")
                PermissionStatus.Denied(deniedPermissions = result.deniedPermissions)
            }
            is PermissionHandler.PermissionResult.SHOW_RATIONALE -> {
                Log.d(TAG, "Permission rationale required for: ${result.permissions}")
                PermissionStatus.RequiresRationale(permissions = result.permissions)
            }
        }
    }
}