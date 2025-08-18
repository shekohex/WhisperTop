package me.shadykhalifa.whispertop.domain.models

/**
 * Domain model representing the overall readiness state of services.
 * This combines service connection status and permission status to determine
 * if the app is ready for recording operations.
 */
data class ServiceReadinessState(
    val serviceConnectionStatus: ServiceConnectionStatus,
    val permissionStatus: PermissionStatus,
    val isReady: Boolean = isServiceReady(serviceConnectionStatus, permissionStatus),
    val errorMessage: String? = null
) {
    companion object {
        /**
         * Determines if services are ready based on connection and permission status.
         */
        private fun isServiceReady(
            connectionStatus: ServiceConnectionStatus,
            permissionStatus: PermissionStatus
        ): Boolean {
            val isConnected = when (connectionStatus) {
                is ServiceConnectionStatus.Connected,
                is ServiceConnectionStatus.AlreadyConnected -> true
                is ServiceConnectionStatus.Failed,
                is ServiceConnectionStatus.Error -> false
            }
            
            val hasPermissions = when (permissionStatus) {
                is PermissionStatus.Granted -> true
                is PermissionStatus.Denied,
                is PermissionStatus.RequiresRationale -> false
            }
            
            return isConnected && hasPermissions
        }
        
        /**
         * Creates a ready state when both service and permissions are available.
         */
        fun ready(
            serviceConnectionStatus: ServiceConnectionStatus = ServiceConnectionStatus.Connected,
            permissionStatus: PermissionStatus = PermissionStatus.Granted
        ) = ServiceReadinessState(
            serviceConnectionStatus = serviceConnectionStatus,
            permissionStatus = permissionStatus,
            isReady = true
        )
        
        /**
         * Creates a not ready state with an error message.
         */
        fun notReady(
            serviceConnectionStatus: ServiceConnectionStatus,
            permissionStatus: PermissionStatus,
            errorMessage: String? = null
        ) = ServiceReadinessState(
            serviceConnectionStatus = serviceConnectionStatus,
            permissionStatus = permissionStatus,
            isReady = false,
            errorMessage = errorMessage
        )
    }
}