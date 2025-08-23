package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.StateFlow
import me.shadykhalifa.whispertop.utils.Result

interface IAudioServiceManager {
    val connectionState: StateFlow<ServiceConnectionState>
    
    suspend fun bindService(): Result<ServiceBindResult>
    fun unbindService()
    fun isServiceBound(): Boolean
    fun cleanup()
    
    enum class ServiceConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED
    }
    
    sealed class ServiceBindResult {
        object Success : ServiceBindResult()
        object Failed : ServiceBindResult()
        object AlreadyBound : ServiceBindResult()
        data class Error(val exception: Exception) : ServiceBindResult()
    }
}