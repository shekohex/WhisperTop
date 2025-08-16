package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.ConnectionStatus
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

interface ConnectionStatusService {
    val connectionStatus: StateFlow<ConnectionStatus>
    fun updateStatus(status: ConnectionStatus)
    fun startMonitoring(scope: CoroutineScope)
    fun stopMonitoring()
    suspend fun checkConnection(): Boolean
}

class ConnectionStatusServiceImpl(
    private val retryService: RetryService
) : ConnectionStatusService {
    
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.UNKNOWN)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus
    
    private var monitoringScope: CoroutineScope? = null
    private var isMonitoring = false
    
    override fun updateStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }
    
    override fun startMonitoring(scope: CoroutineScope) {
        if (isMonitoring) return
        
        monitoringScope = scope
        isMonitoring = true
        
        scope.launch {
            while (isActive && isMonitoring) {
                try {
                    _connectionStatus.value = ConnectionStatus.CONNECTING
                    val isConnected = checkConnection()
                    _connectionStatus.value = if (isConnected) {
                        ConnectionStatus.CONNECTED
                    } else {
                        ConnectionStatus.DISCONNECTED
                    }
                } catch (e: Exception) {
                    _connectionStatus.value = ConnectionStatus.DISCONNECTED
                }
                
                delay(30_000) // Check every 30 seconds
            }
        }
    }
    
    override fun stopMonitoring() {
        isMonitoring = false
        monitoringScope = null
    }
    
    override suspend fun checkConnection(): Boolean {
        return try {
            retryService.withRetry(
                maxRetries = 2,
                initialDelay = 1000L,
                exponentialBackoff = false
            ) {
                // Simple network check - attempt to resolve a reliable host
                performNetworkCheck()
            }
            true
        } catch (e: Exception) {
            false
        }
    }
    
    private suspend fun performNetworkCheck(): Boolean {
        // This is a placeholder - in a real implementation, you might:
        // 1. Make a lightweight HTTP request to a reliable endpoint
        // 2. Check if OpenAI API is reachable
        // 3. Verify internet connectivity
        
        delay(100) // Simulate network check
        return true // For now, assume connection is available
    }
}