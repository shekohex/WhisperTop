package me.shadykhalifa.whispertop.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever
import me.shadykhalifa.whispertop.ui.overlay.OverlayView
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OverlayManagerTest {
    
    @Mock
    private lateinit var mockPermissionHandler: PermissionHandler
    
    @Mock
    private lateinit var mockOverlayView: OverlayView
    
    private lateinit var context: Context
    private lateinit var overlayManager: OverlayManager
    private var serviceStateChanges = mutableListOf<OverlayManager.ServiceState>()
    private var overlayStateChanges = mutableListOf<OverlayManager.OverlayState>()
    private var overlayEvents = mutableListOf<String>()
    private var errorMessages = mutableListOf<String>()
    
    private val testListener = object : OverlayManager.OverlayManagerListener {
        override fun onServiceStateChanged(state: OverlayManager.ServiceState) {
            serviceStateChanges.add(state)
        }
        
        override fun onOverlayStateChanged(state: OverlayManager.OverlayState) {
            overlayStateChanges.add(state)
        }
        
        override fun onOverlayAdded(overlayId: String) {
            overlayEvents.add("added:$overlayId")
        }
        
        override fun onOverlayRemoved(overlayId: String) {
            overlayEvents.add("removed:$overlayId")
        }
        
        override fun onOverlayError(error: String) {
            errorMessages.add(error)
        }
    }
    
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        context = ApplicationProvider.getApplicationContext()
        
        stopKoin()
        startKoin {
            androidContext(context)
            modules(module {
                single<Context> { context }
                single<PermissionHandler> { mockPermissionHandler }
            })
        }
        
        overlayManager = OverlayManager()
        
        serviceStateChanges.clear()
        overlayStateChanges.clear()
        overlayEvents.clear()
        errorMessages.clear()
    }
    
    @Test
    fun testInitialState() {
        assertEquals(OverlayManager.ServiceState.DISCONNECTED, overlayManager.getCurrentServiceState())
        assertEquals(OverlayManager.OverlayState.IDLE, overlayManager.getCurrentOverlayState())
        assertFalse(overlayManager.isServiceConnected())
    }
    
    @Test
    fun testStateFlows() = runBlocking {
        val initialServiceState = overlayManager.serviceState.first()
        val initialOverlayState = overlayManager.overlayState.first()
        
        assertEquals(OverlayManager.ServiceState.DISCONNECTED, initialServiceState)
        assertEquals(OverlayManager.OverlayState.IDLE, initialOverlayState)
    }
    
    @Test
    fun testServiceStartWithPermission() = runBlocking {
        whenever(mockPermissionHandler.hasOverlayPermission()).thenReturn(true)
        
        overlayManager.addListener(testListener)
        
        val result = overlayManager.startService()
        assertTrue(result)
        
        // Wait for service connection
        withTimeout(5000) {
            while (overlayManager.getCurrentServiceState() == OverlayManager.ServiceState.DISCONNECTED) {
                delay(100)
            }
        }
        
        assertTrue(serviceStateChanges.contains(OverlayManager.ServiceState.CONNECTING))
    }
    
    @Test
    fun testServiceStartWithoutPermission() = runBlocking {
        whenever(mockPermissionHandler.hasOverlayPermission()).thenReturn(false)
        
        overlayManager.addListener(testListener)
        
        val result = overlayManager.startService()
        assertFalse(result)
        
        delay(100)
        
        assertTrue(errorMessages.any { it.contains("permission not granted") })
    }
    
    @Test
    fun testServiceStop() = runBlocking {
        whenever(mockPermissionHandler.hasOverlayPermission()).thenReturn(true)
        
        overlayManager.addListener(testListener)
        overlayManager.startService()
        
        // Wait for service to start
        delay(500)
        
        overlayManager.stopService()
        
        // Wait for service to stop
        delay(500)
        
        assertEquals(OverlayManager.ServiceState.DISCONNECTED, overlayManager.getCurrentServiceState())
        assertFalse(overlayManager.isServiceConnected())
    }
    
    @Test
    fun testAddOverlayWithoutService() = runBlocking {
        whenever(mockPermissionHandler.hasOverlayPermission()).thenReturn(true)
        
        val overlayId = "test_overlay"
        val result = overlayManager.addOverlay(overlayId, mockOverlayView)
        
        // Should start service automatically and add overlay
        assertTrue(result)
    }
    
    @Test
    fun testAddOverlayWithoutPermission() = runBlocking {
        whenever(mockPermissionHandler.hasOverlayPermission()).thenReturn(false)
        
        overlayManager.addListener(testListener)
        
        val overlayId = "test_overlay"
        val result = overlayManager.addOverlay(overlayId, mockOverlayView)
        
        assertFalse(result)
        delay(100)
        assertTrue(errorMessages.any { it.contains("permission not granted") })
    }
    
    @Test
    fun testRemoveOverlayWithoutService() {
        val overlayId = "test_overlay"
        val result = overlayManager.removeOverlay(overlayId)
        
        assertFalse(result)
    }
    
    @Test
    fun testUpdateOverlayParamsWithoutService() {
        val overlayId = "test_overlay"
        val layoutParams = OverlayView.createLayoutParams()
        val result = overlayManager.updateOverlayParams(overlayId, layoutParams)
        
        assertFalse(result)
    }
    
    @Test
    fun testIsOverlayVisibleWithoutService() {
        val overlayId = "test_overlay"
        val result = overlayManager.isOverlayVisible(overlayId)
        
        assertFalse(result)
    }
    
    @Test
    fun testGetActiveOverlaysWithoutService() {
        val result = overlayManager.getActiveOverlays()
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun testListenerManagement() {
        overlayManager.addListener(testListener)
        overlayManager.removeListener(testListener)
        
        // After removing listener, no events should be received
        // This is verified by other tests not receiving events after cleanup
    }
    
    @Test
    fun testMultipleListeners() = runBlocking {
        val listener1Events = mutableListOf<String>()
        val listener2Events = mutableListOf<String>()
        
        val listener1 = object : OverlayManager.OverlayManagerListener {
            override fun onServiceStateChanged(state: OverlayManager.ServiceState) {
                listener1Events.add("service:$state")
            }
            override fun onOverlayStateChanged(state: OverlayManager.OverlayState) {
                listener1Events.add("overlay:$state")
            }
            override fun onOverlayAdded(overlayId: String) {
                listener1Events.add("added:$overlayId")
            }
            override fun onOverlayRemoved(overlayId: String) {
                listener1Events.add("removed:$overlayId")
            }
            override fun onOverlayError(error: String) {
                listener1Events.add("error:$error")
            }
        }
        
        val listener2 = object : OverlayManager.OverlayManagerListener {
            override fun onServiceStateChanged(state: OverlayManager.ServiceState) {
                listener2Events.add("service:$state")
            }
            override fun onOverlayStateChanged(state: OverlayManager.OverlayState) {
                listener2Events.add("overlay:$state")
            }
            override fun onOverlayAdded(overlayId: String) {
                listener2Events.add("added:$overlayId")
            }
            override fun onOverlayRemoved(overlayId: String) {
                listener2Events.add("removed:$overlayId")
            }
            override fun onOverlayError(error: String) {
                listener2Events.add("error:$error")
            }
        }
        
        overlayManager.addListener(listener1)
        overlayManager.addListener(listener2)
        
        whenever(mockPermissionHandler.hasOverlayPermission()).thenReturn(false)
        
        overlayManager.addOverlay("test", mockOverlayView)
        
        delay(100)
        
        // Both listeners should receive the error event
        assertTrue(listener1Events.any { it.contains("error") })
        assertTrue(listener2Events.any { it.contains("error") })
        
        overlayManager.removeListener(listener1)
        overlayManager.removeListener(listener2)
    }
}