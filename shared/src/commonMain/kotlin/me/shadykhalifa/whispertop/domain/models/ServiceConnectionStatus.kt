package me.shadykhalifa.whispertop.domain.models

/**
 * Domain model representing the status of service connection.
 * This replaces infrastructure types from AudioServiceManager.ServiceBindResult.
 */
sealed class ServiceConnectionStatus {
    /**
     * Service has been successfully connected and is ready for use.
     */
    object Connected : ServiceConnectionStatus()
    
    /**
     * Service was already connected when connection was attempted.
     */
    object AlreadyConnected : ServiceConnectionStatus()
    
    /**
     * Service connection failed for unknown reasons.
     */
    object Failed : ServiceConnectionStatus()
    
    /**
     * Service connection failed with a specific error.
     */
    data class Error(val message: String, val cause: Throwable? = null) : ServiceConnectionStatus()
}