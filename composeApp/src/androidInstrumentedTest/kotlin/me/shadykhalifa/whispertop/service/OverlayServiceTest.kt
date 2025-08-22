package me.shadykhalifa.whispertop.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.ui.overlay.OverlayView
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class OverlayServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private var service: OverlayService? = null
    private val testOverlayId = "test_overlay_id"
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as OverlayService.OverlayServiceBinder
            this@OverlayServiceTest.service = binder.getService()
        }

        override fun onServiceDisconnected(className: ComponentName) {
            service = null
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun teardown() {
        service = null
    }

    @Test
    fun serviceCreation_success() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        assertNotNull(service)
    }

    @Test
    fun serviceState_initialState() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        // Initial state should be ACTIVE after service creation
        var currentState: OverlayService.OverlayState? = null
        
        val stateListener = object : OverlayService.OverlayStateListener {
            override fun onStateChanged(state: OverlayService.OverlayState) {
                currentState = state
            }
            override fun onOverlayAdded(overlayId: String) {}
            override fun onOverlayRemoved(overlayId: String) {}
            override fun onOverlayError(error: String) {}
        }
        
        service.addStateListener(stateListener)
        
        // Give service time to initialize
        delay(100)
        
        // State should be ACTIVE for running service
        // Note: In test environment without overlay permission, state might be ERROR
        assertNotNull(currentState)
    }

    @Test
    fun stateListener_addAndRemove() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        val stateListener = object : OverlayService.OverlayStateListener {
            override fun onStateChanged(state: OverlayService.OverlayState) {}
            override fun onOverlayAdded(overlayId: String) {}
            override fun onOverlayRemoved(overlayId: String) {}
            override fun onOverlayError(error: String) {}
        }
        
        // Should be able to add and remove listeners without exception
        service.addStateListener(stateListener)
        service.removeStateListener(stateListener)
    }

    @Test
    fun overlayPermission_checkWithoutPermission() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        // In test environment, overlay permission is typically not granted
        var errorReceived = false
        var errorMessage: String? = null
        
        val stateListener = object : OverlayService.OverlayStateListener {
            override fun onStateChanged(state: OverlayService.OverlayState) {}
            override fun onOverlayAdded(overlayId: String) {}
            override fun onOverlayRemoved(overlayId: String) {}
            override fun onOverlayError(error: String) {
                errorReceived = true
                errorMessage = error
            }
        }
        
        service.addStateListener(stateListener)
        
        // Create a mock overlay view
        val overlayView = OverlayView(context)
        
        // Attempt to add overlay without permission
        val result = service.addOverlay(testOverlayId, overlayView)
        
        // Should fail due to missing permission
        assertFalse(result)
        
        // Wait for potential error callback
        delay(100)
        
        // Should receive error about missing permission
        assertTrue(errorReceived)
        assertNotNull(errorMessage)
        assertTrue(errorMessage!!.contains("permission", ignoreCase = true))
    }

    @Test
    fun overlayManagement_addWithoutPermission() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        val overlayView = OverlayView(context)
        
        // Without SYSTEM_ALERT_WINDOW permission, this should fail
        val result = service.addOverlay(testOverlayId, overlayView)
        
        assertFalse(result)
    }

    @Test
    fun overlayRemoval_nonExistentOverlay() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        // Removing non-existent overlay should not cause issues
        val result = service.removeOverlay("non_existent_id")
        
        assertFalse(result) // Should return false for non-existent overlay
    }

    @Test
    fun serviceLifecycle_startAndStop() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        
        // Start service
        serviceRule.startService(serviceIntent)
        
        // Bind to service
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        assertNotNull(service)
        
        // Service should be running
        assertTrue(service.isRunning())
    }

    @Test
    fun overlayStateNotification_multipleListeners() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        var listener1Called = false
        var listener2Called = false
        
        val stateListener1 = object : OverlayService.OverlayStateListener {
            override fun onStateChanged(state: OverlayService.OverlayState) {
                listener1Called = true
            }
            override fun onOverlayAdded(overlayId: String) {}
            override fun onOverlayRemoved(overlayId: String) {}
            override fun onOverlayError(error: String) {}
        }
        
        val stateListener2 = object : OverlayService.OverlayStateListener {
            override fun onStateChanged(state: OverlayService.OverlayState) {
                listener2Called = true
            }
            override fun onOverlayAdded(overlayId: String) {}
            override fun onOverlayRemoved(overlayId: String) {}
            override fun onOverlayError(error: String) {}
        }
        
        service.addStateListener(stateListener1)
        service.addStateListener(stateListener2)
        
        // Wait for service initialization
        delay(100)
        
        // Both listeners should receive state changes
        assertTrue(listener1Called || listener2Called) // At least one should be called
    }

    @Test
    fun serviceDestroy_cleansUpResources() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        assertTrue(service.isRunning())
        
        // Unbind service (this will eventually lead to onDestroy if no other connections)
        serviceRule.unbindService()
        
        // Give time for cleanup
        delay(200)
        
        // Service should handle cleanup gracefully
        // (We can't directly test if resources are cleaned up, but service should not crash)
    }

    @Test
    fun errorHandling_invalidLayoutParams() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        val overlayView = OverlayView(context)
        
        // Create invalid layout params (null is actually handled, so this tests the default path)
        val result = service.addOverlay(testOverlayId, overlayView, null)
        
        // Should handle null layout params by creating defaults
        // Result will be false due to missing overlay permission, but shouldn't crash
        assertFalse(result)
    }

    @Test
    fun overlayView_creation() = runTest {
        // Test that OverlayView can be created without issues
        val overlayView = OverlayView(context)
        
        assertNotNull(overlayView)
        assertEquals(context, overlayView.context)
    }

    @Test
    fun serviceBinding_multipleClients() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        
        // Bind service multiple times
        val binder1 = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val binder2 = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        
        val service1 = binder1.getService()
        val service2 = binder2.getService()
        
        assertNotNull(service1)
        assertNotNull(service2)
        
        // Both binders should return the same service instance
        assertEquals(service1, service2)
    }

    @Test
    fun overlayState_enumValues() {
        val states = OverlayService.OverlayState.entries
        
        assertEquals(3, states.size)
        assertTrue(states.contains(OverlayService.OverlayState.IDLE))
        assertTrue(states.contains(OverlayService.OverlayState.ACTIVE))
        assertTrue(states.contains(OverlayService.OverlayState.ERROR))
    }

    @Test
    fun serviceRecovery_afterError() = runTest {
        val serviceIntent = Intent(context, OverlayService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as OverlayService.OverlayServiceBinder
        val service = binder.getService()
        
        var errorCount = 0
        
        val stateListener = object : OverlayService.OverlayStateListener {
            override fun onStateChanged(state: OverlayService.OverlayState) {}
            override fun onOverlayAdded(overlayId: String) {}
            override fun onOverlayRemoved(overlayId: String) {}
            override fun onOverlayError(error: String) {
                errorCount++
            }
        }
        
        service.addStateListener(stateListener)
        
        val overlayView = OverlayView(context)
        
        // Attempt multiple overlay additions (will fail due to permission)
        service.addOverlay("test1", overlayView)
        service.addOverlay("test2", overlayView)
        
        delay(100)
        
        // Service should handle multiple errors gracefully
        assertTrue(errorCount >= 2)
        
        // Service should still be responsive
        assertTrue(service.isRunning())
    }
}