package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.ConnectionStatus
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.delay
import kotlin.test.Test
import kotlin.test.assertEquals

class ConnectionStatusServiceTest {

    private val retryService = RetryServiceImpl()
    private val connectionStatusService = ConnectionStatusServiceImpl(retryService)

    @Test
    fun `initial connection status should be UNKNOWN`() = runTest {
        val status = connectionStatusService.connectionStatus.first()
        assertEquals(ConnectionStatus.UNKNOWN, status)
    }

    @Test
    fun `updateStatus should update connection status`() = runTest {
        connectionStatusService.updateStatus(ConnectionStatus.CONNECTED)
        
        val status = connectionStatusService.connectionStatus.first()
        assertEquals(ConnectionStatus.CONNECTED, status)
        
        connectionStatusService.updateStatus(ConnectionStatus.DISCONNECTED)
        
        val updatedStatus = connectionStatusService.connectionStatus.first()
        assertEquals(ConnectionStatus.DISCONNECTED, updatedStatus)
    }

    @Test
    fun `checkConnection should return true for successful connection`() = runTest {
        val isConnected = connectionStatusService.checkConnection()
        assertEquals(true, isConnected)
    }

    @Test
    fun `startMonitoring should begin monitoring connection status`() = runTest {
        val testScope = TestScope()
        
        connectionStatusService.startMonitoring(testScope)
        
        // Allow some time for monitoring to start
        delay(100)
        
        connectionStatusService.stopMonitoring()
    }

    @Test
    fun `stopMonitoring should stop monitoring`() = runTest {
        val testScope = TestScope()
        
        connectionStatusService.startMonitoring(testScope)
        connectionStatusService.stopMonitoring()
        
        // No exception should be thrown
    }

    @Test
    fun `multiple startMonitoring calls should not cause issues`() = runTest {
        val testScope = TestScope()
        
        connectionStatusService.startMonitoring(testScope)
        connectionStatusService.startMonitoring(testScope) // Should be ignored
        connectionStatusService.startMonitoring(testScope) // Should be ignored
        
        connectionStatusService.stopMonitoring()
    }

    @Test
    fun `updateStatus should work with all connection status values`() = runTest {
        val statuses = listOf(
            ConnectionStatus.CONNECTED,
            ConnectionStatus.DISCONNECTED,
            ConnectionStatus.CONNECTING,
            ConnectionStatus.UNKNOWN
        )
        
        statuses.forEach { expectedStatus ->
            connectionStatusService.updateStatus(expectedStatus)
            val actualStatus = connectionStatusService.connectionStatus.first()
            assertEquals(expectedStatus, actualStatus)
        }
    }
}

class MockConnectionStatusService : ConnectionStatusService {
    private val _connectionStatus = kotlinx.coroutines.flow.MutableStateFlow(ConnectionStatus.UNKNOWN)
    override val connectionStatus = _connectionStatus
    
    private var shouldFailConnection = false
    
    override fun updateStatus(status: ConnectionStatus) {
        _connectionStatus.value = status
    }
    
    override fun startMonitoring(scope: kotlinx.coroutines.CoroutineScope) {
        // Mock implementation - no actual monitoring
    }
    
    override fun stopMonitoring() {
        // Mock implementation
    }
    
    override suspend fun checkConnection(): Boolean {
        return !shouldFailConnection
    }
    
    fun setConnectionFailure(shouldFail: Boolean) {
        shouldFailConnection = shouldFail
    }
}

class MockConnectionStatusServiceTest {

    private val mockService = MockConnectionStatusService()

    @Test
    fun `mock service should handle connection failure`() = runTest {
        // Initially should succeed
        assertEquals(true, mockService.checkConnection())
        
        // Set to fail
        mockService.setConnectionFailure(true)
        assertEquals(false, mockService.checkConnection())
        
        // Set back to succeed
        mockService.setConnectionFailure(false)
        assertEquals(true, mockService.checkConnection())
    }

    @Test
    fun `mock service should update status correctly`() = runTest {
        mockService.updateStatus(ConnectionStatus.CONNECTED)
        assertEquals(ConnectionStatus.CONNECTED, mockService.connectionStatus.first())
        
        mockService.updateStatus(ConnectionStatus.DISCONNECTED)
        assertEquals(ConnectionStatus.DISCONNECTED, mockService.connectionStatus.first())
    }
}