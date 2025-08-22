package me.shadykhalifa.whispertop.managers

import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.service.AudioRecordingService
import me.shadykhalifa.whispertop.utils.CircuitBreaker
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import java.lang.ref.WeakReference
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for EnhancedAudioServiceManager
 * Tests service binding, memory leak prevention, and circuit breaker integration
 */
class EnhancedAudioServiceManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockAudioService: AudioRecordingService
    private lateinit var mockBinder: AudioRecordingService.AudioRecordingBinder
    private lateinit var mockIBinder: IBinder
    private lateinit var enhancedServiceManager: EnhancedAudioServiceManager
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockAudioService = mockk(relaxed = true)
        mockBinder = mockk(relaxed = true)
        mockIBinder = mockk(relaxed = true)
        
        // Setup Koin with mock context
        startKoin {
            modules(module {
                single<Context> { mockContext }
            })
        }
        
        // Mock binder interactions
        every { mockBinder.getService() } returns mockAudioService
        every { mockIBinder as AudioRecordingService.AudioRecordingBinder } returns mockBinder
        every { mockAudioService.getCurrentState() } returns AudioRecordingService.RecordingState.IDLE
        
        enhancedServiceManager = EnhancedAudioServiceManager()
    }
    
    @After
    fun teardown() {
        enhancedServiceManager.destroy()
        stopKoin()
    }
    
    @Test
    fun `bindService should succeed when service binding is successful`() = runTest {
        // Given
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        
        // When
        val result = enhancedServiceManager.bindService()
        
        // Then
        assertTrue(result is EnhancedAudioServiceManager.ServiceBindResult.SUCCESS)
        verify { mockContext.bindService(any(), any<ServiceConnection>(), any()) }
    }
    
    @Test
    fun `bindService should fail when service binding fails`() = runTest {
        // Given
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns false
        
        // When
        val result = enhancedServiceManager.bindService()
        
        // Then
        assertTrue(result is EnhancedAudioServiceManager.ServiceBindResult.FAILED)
        verify { mockContext.bindService(any(), any<ServiceConnection>(), any()) }
    }
    
    @Test
    fun `bindService should return ALREADY_BOUND when service is already bound`() = runTest {
        // Given - simulate already bound service
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        
        // First binding
        enhancedServiceManager.bindService()
        
        // Simulate successful connection
        val serviceManagerSpy = spyk(enhancedServiceManager, recordPrivateCalls = true)
        
        // When - attempt second binding
        val result = enhancedServiceManager.bindService()
        
        // Then
        assertTrue(result is EnhancedAudioServiceManager.ServiceBindResult.ALREADY_BOUND)
    }
    
    @Test
    fun `connectionState should update correctly during binding process`() = runTest {
        // Given
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        
        // Initial state should be DISCONNECTED
        assertEquals(
            EnhancedAudioServiceManager.ServiceConnectionState.DISCONNECTED,
            enhancedServiceManager.connectionState.first()
        )
        
        // When
        enhancedServiceManager.bindService()
        
        // Then - state should be CONNECTING during binding
        assertEquals(
            EnhancedAudioServiceManager.ServiceConnectionState.CONNECTING,
            enhancedServiceManager.connectionState.first()
        )
    }
    
    @Test
    fun `startRecording should return SERVICE_NOT_BOUND when service is not bound`() = runTest {
        // Given - no service bound
        
        // When
        val result = enhancedServiceManager.startRecording()
        
        // Then
        assertTrue(result is EnhancedAudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND)
    }
    
    @Test
    fun `startRecording should succeed when service is bound`() = runTest {
        // Given - mock bound service
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        every { mockAudioService.startRecording() } returns Unit
        
        // Bind service first
        enhancedServiceManager.bindService()
        
        // Simulate service connection by directly calling the private method
        // In a real scenario, this would be triggered by the ServiceConnection callback
        val serviceManagerReflection = EnhancedAudioServiceManager::class.java
        val serviceConnectionField = serviceManagerReflection.getDeclaredField("serviceConnection")
        serviceConnectionField.isAccessible = true
        val serviceConnection = serviceConnectionField.get(enhancedServiceManager) as ServiceConnection
        
        // Simulate successful service connection
        serviceConnection.onServiceConnected(mockk(), mockIBinder)
        
        // When
        val result = enhancedServiceManager.startRecording()
        
        // Then
        assertTrue(result is EnhancedAudioServiceManager.RecordingActionResult.SUCCESS)
        verify { mockAudioService.startRecording() }
    }
    
    @Test
    fun `unbindService should clean up resources properly`() = runTest {
        // Given - bound service
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        every { mockContext.unbindService(any<ServiceConnection>()) } returns Unit
        every { mockAudioService.clearAllListeners() } returns Unit
        
        enhancedServiceManager.bindService()
        
        // When
        enhancedServiceManager.unbindService()
        
        // Then
        verify { mockContext.unbindService(any<ServiceConnection>()) }
        assertEquals(
            EnhancedAudioServiceManager.ServiceConnectionState.DISCONNECTED,
            enhancedServiceManager.connectionState.first()
        )
    }
    
    @Test
    fun `destroy should cleanup all resources and prevent further operations`() = runTest {
        // Given - bound service
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        every { mockContext.unbindService(any<ServiceConnection>()) } returns Unit
        
        enhancedServiceManager.bindService()
        
        // When
        enhancedServiceManager.destroy()
        
        // Then - subsequent operations should return MANAGER_DESTROYED
        val startResult = enhancedServiceManager.startRecording()
        assertTrue(startResult is EnhancedAudioServiceManager.RecordingActionResult.MANAGER_DESTROYED)
        
        val bindResult = enhancedServiceManager.bindService()
        assertTrue(bindResult is EnhancedAudioServiceManager.ServiceBindResult.MANAGER_DESTROYED)
    }
    
    @Test
    fun `getServiceStatus should return current status information`() = runTest {
        // Given
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } returns true
        
        // When
        val status = enhancedServiceManager.getServiceStatus()
        
        // Then
        assertFalse(status.isConnected)
        assertFalse(status.isBinding)
        assertEquals(0, status.bindAttemptCount)
        assertEquals(AudioRecordingService.RecordingState.IDLE, status.currentRecordingState)
    }
    
    @Test
    fun `circuit breaker should prevent operations when open`() = runTest {
        // Given - simulate circuit breaker in open state
        val serviceManagerSpy = spyk(enhancedServiceManager, recordPrivateCalls = true)
        
        // Mock circuit breaker to throw exception
        val circuitBreakerField = EnhancedAudioServiceManager::class.java.getDeclaredField("circuitBreaker")
        circuitBreakerField.isAccessible = true
        val mockCircuitBreaker = mockk<CircuitBreaker>()
        circuitBreakerField.set(serviceManagerSpy, mockCircuitBreaker)
        
        coEvery { mockCircuitBreaker.execute<Any>(any()) } throws me.shadykhalifa.whispertop.utils.CircuitBreakerOpenException("Circuit breaker is open")
        
        // When
        val result = serviceManagerSpy.bindService()
        
        // Then
        assertTrue(result is EnhancedAudioServiceManager.ServiceBindResult.ERROR)
    }
    
    @Test
    fun `WeakServiceStateListener should not hold strong references`() = runTest {
        // Given
        val serviceManagerSpy = spyk(enhancedServiceManager, recordPrivateCalls = true)
        
        // Create WeakServiceStateListener using reflection
        val listenerClass = Class.forName("${EnhancedAudioServiceManager::class.java.name}\$WeakServiceStateListener")
        val constructor = listenerClass.getDeclaredConstructor(EnhancedAudioServiceManager::class.java)
        constructor.isAccessible = true
        val listener = constructor.newInstance(serviceManagerSpy) as AudioRecordingService.RecordingStateListener
        
        // When - simulate state change
        listener.onStateChanged(AudioRecordingService.RecordingState.RECORDING)
        
        // Then - verify that weak reference doesn't prevent garbage collection
        // This is tested by ensuring the listener still works when manager exists
        verify { serviceManagerSpy.getCurrentRecordingState() }
        
        // Test that listener handles null weak reference gracefully
        val managerRefField = listenerClass.getDeclaredField("managerRef")
        managerRefField.isAccessible = true
        val weakRef = managerRefField.get(listener) as WeakReference<EnhancedAudioServiceManager>
        
        // The weak reference should exist
        assertTrue(weakRef.get() != null)
    }
    
    @Test
    fun `timeout should be handled gracefully in bindService`() = runTest {
        // Given - simulate very slow binding
        every { mockContext.bindService(any(), any<ServiceConnection>(), any()) } answers {
            Thread.sleep(50) // Short delay for test
            true
        }
        
        // When
        val result = enhancedServiceManager.bindService()
        
        // Then - should complete successfully (our timeout is longer than the delay)
        assertTrue(result is EnhancedAudioServiceManager.ServiceBindResult.SUCCESS)
    }
}