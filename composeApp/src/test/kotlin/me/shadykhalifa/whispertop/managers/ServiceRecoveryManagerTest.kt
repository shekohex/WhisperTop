package me.shadykhalifa.whispertop.managers

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class ServiceRecoveryManagerTest {
    
    @Mock
    private lateinit var mockAudioServiceManager: AudioServiceManager
    
    @Mock
    private lateinit var mockPermissionHandler: PermissionHandler
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var recoveryManager: ServiceRecoveryManager
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup default mock behavior
        whenever(mockAudioServiceManager.connectionState).thenReturn(
            MutableStateFlow(AudioServiceManager.ServiceConnectionState.DISCONNECTED)
        )
        whenever(mockPermissionHandler.permissionState).thenReturn(
            MutableStateFlow(PermissionHandler.PermissionState.UNKNOWN)
        )
    }
    
    @Test
    fun testRecoveryManagerCreation() {
        assertNotNull(recoveryManager)
    }
    
    @Test
    fun testInitialRecoveryState() = testScope.runTest {
        val initialState = recoveryManager.recoveryState.value
        assertEquals(ServiceRecoveryManager.RecoveryState.IDLE, initialState)
    }
    
    @Test
    fun testResetRecoveryState() {
        recoveryManager.reset()
        
        val state = recoveryManager.recoveryState.value
        assertEquals(ServiceRecoveryManager.RecoveryState.IDLE, state)
    }
    
    @Test
    fun testHandleServiceCrash() = testScope.runTest {
        recoveryManager.handleServiceCrash()
        
        // Should immediately set state to crashed
        val state = recoveryManager.recoveryState.value
        assertEquals(ServiceRecoveryManager.RecoveryState.SERVICE_CRASHED, state)
    }
    
    @Test
    fun testHandlePermissionDenied() = testScope.runTest {
        val deniedPermissions = listOf("android.permission.RECORD_AUDIO")
        
        recoveryManager.handlePermissionDenied(deniedPermissions)
        
        val state = recoveryManager.recoveryState.value
        assertEquals(ServiceRecoveryManager.RecoveryState.PERMISSION_DENIED, state)
    }
    
    @Test
    fun testCleanupDoesNotThrow() {
        recoveryManager.cleanup()
        // Should not throw any exceptions
    }
}