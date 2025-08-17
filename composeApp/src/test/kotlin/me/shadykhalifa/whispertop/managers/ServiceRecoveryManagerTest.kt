package me.shadykhalifa.whispertop.managers

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ServiceRecoveryManagerTest : KoinTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockAudioServiceManager: AudioServiceManager
    private lateinit var mockPermissionHandler: PermissionHandler
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private lateinit var recoveryManager: ServiceRecoveryManager
    
    @Before
    fun setup() {
        stopKoin()
        
        mockContext = mock()
        mockAudioServiceManager = mock()
        mockPermissionHandler = mock()
        
        // Setup default mock behavior
        whenever(mockAudioServiceManager.connectionState).thenReturn(
            MutableStateFlow(AudioServiceManager.ServiceConnectionState.DISCONNECTED)
        )
        whenever(mockPermissionHandler.permissionState).thenReturn(
            MutableStateFlow(PermissionHandler.PermissionState.UNKNOWN)
        )
        
        startKoin {
            modules(
                module {
                    single<Context> { mockContext }
                    single<AudioServiceManager> { mockAudioServiceManager }
                    single<PermissionHandler> { mockPermissionHandler }
                }
            )
        }
        
        recoveryManager = ServiceRecoveryManager(mockContext)
    }
    
    @After
    fun tearDown() {
        recoveryManager.cleanup()
        stopKoin()
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
        
        // Advance the test dispatcher to complete the coroutine
        testDispatcher.scheduler.advanceUntilIdle()
        
        val state = recoveryManager.recoveryState.value
        assertEquals(ServiceRecoveryManager.RecoveryState.REQUIRES_MANUAL_INTERVENTION, state)
    }
    
    @Test
    fun testCleanupDoesNotThrow() {
        recoveryManager.cleanup()
        // Should not throw any exceptions
    }
}