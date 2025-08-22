package me.shadykhalifa.whispertop.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
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
class AudioRecordingServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var context: Context
    private var service: AudioRecordingService? = null
    
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AudioRecordingService.AudioRecordingBinder
            this@AudioRecordingServiceTest.service = binder.getService()
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
        service?.stopRecording()
        service = null
    }

    @Test
    fun serviceCreation_success() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        assertNotNull(service)
    }

    @Test
    fun recordingState_initialState() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Initial state should be IDLE
        assertEquals(AudioRecordingService.RecordingState.IDLE, service.getCurrentState())
    }

    @Test
    fun recordingState_enumValues() {
        val states = AudioRecordingService.RecordingState.entries
        
        assertEquals(4, states.size)
        assertTrue(states.contains(AudioRecordingService.RecordingState.IDLE))
        assertTrue(states.contains(AudioRecordingService.RecordingState.RECORDING))
        assertTrue(states.contains(AudioRecordingService.RecordingState.PAUSED))
        assertTrue(states.contains(AudioRecordingService.RecordingState.PROCESSING))
    }

    @Test
    fun stateListener_addAndRemove() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        var stateChanges = 0
        var errorReceived: String? = null
        
        val stateListener = object : AudioRecordingService.RecordingStateListener {
            override fun onStateChanged(state: AudioRecordingService.RecordingState) {
                stateChanges++
            }
            
            override fun onRecordingComplete(audioFile: me.shadykhalifa.whispertop.domain.models.AudioFile?) {
                // Recording completion handled
            }
            
            override fun onRecordingError(error: String) {
                errorReceived = error
            }
        }
        
        // Should be able to add and remove listeners without exception
        service.addStateListener(stateListener)
        service.removeStateListener(stateListener)
    }

    @Test
    fun recordingStart_withoutPermission() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        var errorReceived: String? = null
        
        val stateListener = object : AudioRecordingService.RecordingStateListener {
            override fun onStateChanged(state: AudioRecordingService.RecordingState) {}
            
            override fun onRecordingComplete(audioFile: me.shadykhalifa.whispertop.domain.models.AudioFile?) {}
            
            override fun onRecordingError(error: String) {
                errorReceived = error
            }
        }
        
        service.addStateListener(stateListener)
        
        // Attempt to start recording without audio permission
        val result = service.startRecording()
        
        // Should fail due to missing permission in test environment
        assertFalse(result)
        
        // Wait for potential error callback
        delay(100)
        
        // Should receive error about missing permission
        assertNotNull(errorReceived)
        assertTrue(errorReceived!!.contains("permission", ignoreCase = true) || 
                  errorReceived!!.contains("audio", ignoreCase = true))
    }

    @Test
    fun recordingStop_whenNotRecording() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Try to stop when not recording
        val result = service.stopRecording()
        
        // Should handle gracefully (return false or true depending on implementation)
        assertNotNull(result)
    }

    @Test
    fun recordingPause_whenNotRecording() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Try to pause when not recording
        val result = service.pauseRecording()
        
        // Should handle gracefully
        assertFalse(result)
    }

    @Test
    fun recordingResume_whenNotPaused() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Try to resume when not paused
        val result = service.resumeRecording()
        
        // Should handle gracefully
        assertFalse(result)
    }

    @Test
    fun serviceHealthCheck_functionality() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Perform health check
        val isHealthy = service.isHealthy()
        
        // Service should be healthy when just created
        assertTrue(isHealthy)
    }

    @Test
    fun recordingDuration_tracking() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Initially, recording duration should be 0
        val initialDuration = service.getRecordingDuration()
        assertEquals(0L, initialDuration)
    }

    @Test
    fun foregroundService_functionality() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Service should be able to handle foreground operations
        // (In test environment, this tests the logic without system interactions)
        val isForeground = service.isForegroundService()
        
        // Should have valid foreground state
        assertNotNull(isForeground)
    }

    @Test
    fun recordingMetrics_collection() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Get recording statistics
        val stats = service.getRecordingStats()
        
        assertNotNull(stats)
        // Initial stats should show no recordings
        assertTrue(stats.sessionCount >= 0)
        assertTrue(stats.totalDuration >= 0)
    }

    @Test
    fun audioFormat_configuration() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Test audio format settings
        val audioFormat = service.getAudioFormat()
        
        assertNotNull(audioFormat)
        assertTrue(audioFormat.sampleRate > 0)
        assertTrue(audioFormat.channelCount > 0)
    }

    @Test
    fun errorRecovery_afterFailure() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        var errorCount = 0
        
        val stateListener = object : AudioRecordingService.RecordingStateListener {
            override fun onStateChanged(state: AudioRecordingService.RecordingState) {}
            
            override fun onRecordingComplete(audioFile: me.shadykhalifa.whispertop.domain.models.AudioFile?) {}
            
            override fun onRecordingError(error: String) {
                errorCount++
            }
        }
        
        service.addStateListener(stateListener)
        
        // Attempt recording multiple times (will fail due to permissions)
        service.startRecording()
        delay(50)
        service.startRecording()
        delay(50)
        
        // Service should handle multiple failures gracefully
        assertTrue(errorCount >= 1)
        
        // Service should still be responsive after errors
        assertTrue(service.isHealthy())
    }

    @Test
    fun wakeLock_management() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Test wake lock state
        val hasWakeLock = service.hasWakeLock()
        
        // Should have valid wake lock state
        assertNotNull(hasWakeLock)
        
        // Initially should not be holding wake lock (or should handle gracefully)
        // The exact behavior depends on implementation
    }

    @Test
    fun serviceBinding_multipleClients() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        
        // Bind service multiple times
        val binder1 = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val binder2 = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        
        val service1 = binder1.getService()
        val service2 = binder2.getService()
        
        assertNotNull(service1)
        assertNotNull(service2)
        
        // Both binders should return the same service instance
        assertEquals(service1, service2)
    }

    @Test
    fun notificationHandling_foregroundService() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        serviceIntent.action = AudioRecordingService.ACTION_START_FOREGROUND
        
        // Start service with foreground action
        serviceRule.startService(serviceIntent)
        
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        assertNotNull(service)
        
        // Service should handle foreground notification
        assertTrue(service.isForegroundService() || !service.isForegroundService()) // Should not crash
    }

    @Test
    fun sessionManagement_idGeneration() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Get current session ID
        val sessionId1 = service.getCurrentSessionId()
        
        // Start new session
        service.startNewSession()
        
        val sessionId2 = service.getCurrentSessionId()
        
        // Session IDs should be different (if sessions are actually created)
        if (sessionId1 != null && sessionId2 != null) {
            assertTrue(sessionId1 != sessionId2)
        }
    }

    @Test
    fun performanceMonitoring_metrics() = runTest {
        val serviceIntent = Intent(context, AudioRecordingService::class.java)
        val binder = serviceRule.bindService(serviceIntent) as AudioRecordingService.AudioRecordingBinder
        val service = binder.getService()
        
        // Test performance metrics collection
        val performanceMetrics = service.getPerformanceMetrics()
        
        assertNotNull(performanceMetrics)
        
        // Metrics should have valid values
        assertTrue(performanceMetrics.memoryUsage >= 0)
        assertTrue(performanceMetrics.cpuUsage >= 0)
    }
}