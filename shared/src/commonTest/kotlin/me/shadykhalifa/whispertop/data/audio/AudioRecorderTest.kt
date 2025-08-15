package me.shadykhalifa.whispertop.data.audio

import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AudioFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class AudioRecorderTest {
    
    @Test
    fun testAudioConfiguration() {
        val config = AudioConfiguration()
        assertEquals(16000, config.sampleRate)
        assertEquals(1, config.channelConfig)
        assertEquals(16, config.audioFormat)
        assertEquals(4, config.bufferSizeMultiplier)
        
        val customConfig = AudioConfiguration(
            sampleRate = 44100,
            channelConfig = 2,
            audioFormat = 24,
            bufferSizeMultiplier = 8
        )
        assertEquals(44100, customConfig.sampleRate)
        assertEquals(2, customConfig.channelConfig)
        assertEquals(24, customConfig.audioFormat)
        assertEquals(8, customConfig.bufferSizeMultiplier)
    }
    
    @Test
    fun testAudioRecordingErrorTypes() {
        val permissionError = AudioRecordingError.PermissionDenied()
        assertEquals("Audio recording permission denied", permissionError.message)
        assertNull(permissionError.cause)
        
        val deviceError = AudioRecordingError.DeviceUnavailable(RuntimeException("Test"))
        assertEquals("Audio recording device unavailable", deviceError.message)
        assertNotNull(deviceError.cause)
        
        val configError = AudioRecordingError.ConfigurationError()
        assertEquals("Audio configuration error", configError.message)
        
        val ioError = AudioRecordingError.IOError()
        assertEquals("Audio I/O error", ioError.message)
        
        val unknownError = AudioRecordingError.Unknown()
        assertEquals("Unknown recording error", unknownError.message)
    }
    
    @Test
    fun testAudioFocusChangeEnum() {
        val values = AudioFocusChange.values()
        assertEquals(4, values.size)
        assertTrue(values.contains(AudioFocusChange.GAIN))
        assertTrue(values.contains(AudioFocusChange.LOSS))
        assertTrue(values.contains(AudioFocusChange.LOSS_TRANSIENT))
        assertTrue(values.contains(AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK))
    }
    
    @Test
    fun testAudioRecordingResultSealed() {
        val successResult = AudioRecordingResult.Success
        assertEquals(AudioRecordingResult.Success, successResult)
        
        val errorResult = AudioRecordingResult.Error(AudioRecordingError.Unknown())
        assertEquals("Unknown recording error", errorResult.error.message)
    }
    
    @Test
    fun testRecordingStateListener() = runTest {
        var startedCalled = false
        var pausedCalled = false
        var resumedCalled = false
        var stoppedCalled = false
        var errorCalled = false
        var lastError: AudioRecordingError? = null
        
        val listener = object : RecordingStateListener {
            override fun onRecordingStarted() {
                startedCalled = true
            }
            
            override fun onRecordingPaused() {
                pausedCalled = true
            }
            
            override fun onRecordingResumed() {
                resumedCalled = true
            }
            
            override fun onRecordingStopped() {
                stoppedCalled = true
            }
            
            override fun onRecordingError(error: AudioRecordingError) {
                errorCalled = true
                lastError = error
            }
        }
        
        // Test all callback methods
        listener.onRecordingStarted()
        assertTrue(startedCalled)
        
        listener.onRecordingPaused()
        assertTrue(pausedCalled)
        
        listener.onRecordingResumed()
        assertTrue(resumedCalled)
        
        listener.onRecordingStopped()
        assertTrue(stoppedCalled)
        
        val testError = AudioRecordingError.PermissionDenied()
        listener.onRecordingError(testError)
        assertTrue(errorCalled)
        assertEquals(testError, lastError)
    }
    
    @Test
    fun testAudioFocusChangeListener() {
        var lastFocusChange: AudioFocusChange? = null
        
        val listener = object : AudioFocusChangeListener {
            override fun onAudioFocusChange(focusChange: AudioFocusChange) {
                lastFocusChange = focusChange
            }
        }
        
        listener.onAudioFocusChange(AudioFocusChange.GAIN)
        assertEquals(AudioFocusChange.GAIN, lastFocusChange)
        
        listener.onAudioFocusChange(AudioFocusChange.LOSS)
        assertEquals(AudioFocusChange.LOSS, lastFocusChange)
        
        listener.onAudioFocusChange(AudioFocusChange.LOSS_TRANSIENT)
        assertEquals(AudioFocusChange.LOSS_TRANSIENT, lastFocusChange)
        
        listener.onAudioFocusChange(AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK)
        assertEquals(AudioFocusChange.LOSS_TRANSIENT_CAN_DUCK, lastFocusChange)
    }
}

class MockAudioRecorder : AudioRecorder {
    private var recording = false
    private val listeners = mutableListOf<RecordingStateListener>()
    private var mockError: AudioRecordingError? = null
    private var shouldFailStart = false
    private var shouldFailStop = false
    
    fun setMockError(error: AudioRecordingError?) {
        mockError = error
    }
    
    fun setShouldFailStart(fail: Boolean) {
        shouldFailStart = fail
    }
    
    fun setShouldFailStop(fail: Boolean) {
        shouldFailStop = fail
    }
    
    override suspend fun startRecording(outputPath: String): AudioRecordingResult {
        if (shouldFailStart) {
            val error = mockError ?: AudioRecordingError.Unknown()
            return AudioRecordingResult.Error(error)
        }
        
        if (recording) {
            return AudioRecordingResult.Error(
                AudioRecordingError.ConfigurationError(IllegalStateException("Already recording"))
            )
        }
        
        recording = true
        notifyListeners { onRecordingStarted() }
        return AudioRecordingResult.Success
    }
    
    override suspend fun stopRecording(): AudioFile? {
        if (!recording) return null
        
        if (shouldFailStop) {
            recording = false
            notifyListeners { onRecordingStopped() }
            return null
        }
        
        recording = false
        notifyListeners { onRecordingStopped() }
        
        return AudioFile(
            path = "/tmp/test.wav",
            durationMs = 5000,
            sizeBytes = 160000
        )
    }
    
    override suspend fun cancelRecording() {
        if (recording) {
            recording = false
            notifyListeners { onRecordingStopped() }
        }
    }
    
    override fun isRecording(): Boolean = recording
    
    override fun addRecordingStateListener(listener: RecordingStateListener) {
        listeners.add(listener)
    }
    
    override fun removeRecordingStateListener(listener: RecordingStateListener) {
        listeners.remove(listener)
    }
    
    fun simulateAudioFocusLoss() {
        recording = false
        notifyListeners { onRecordingError(AudioRecordingError.DeviceUnavailable()) }
    }
    
    fun simulatePause() {
        if (recording) {
            notifyListeners { onRecordingPaused() }
        }
    }
    
    fun simulateResume() {
        if (recording) {
            notifyListeners { onRecordingResumed() }
        }
    }
    
    private fun notifyListeners(action: RecordingStateListener.() -> Unit) {
        listeners.forEach { it.action() }
    }
}

class MockAudioFocusManager : AudioFocusManager {
    private var listener: AudioFocusChangeListener? = null
    private var shouldGrantFocus = true
    
    fun setShouldGrantFocus(grant: Boolean) {
        shouldGrantFocus = grant
    }
    
    override fun requestAudioFocus(listener: AudioFocusChangeListener): Boolean {
        this.listener = listener
        return shouldGrantFocus
    }
    
    override fun abandonAudioFocus() {
        listener = null
    }
    
    fun simulateAudioFocusChange(change: AudioFocusChange) {
        listener?.onAudioFocusChange(change)
    }
}

class AudioRecorderIntegrationTest {
    
    @Test
    fun testRecorderWithMockDependencies() = runTest {
        val mockRecorder = MockAudioRecorder()
        var recordingStarted = false
        var recordingStopped = false
        var recordingError: AudioRecordingError? = null
        
        val listener = object : RecordingStateListener {
            override fun onRecordingStarted() { recordingStarted = true }
            override fun onRecordingPaused() {}
            override fun onRecordingResumed() {}
            override fun onRecordingStopped() { recordingStopped = true }
            override fun onRecordingError(error: AudioRecordingError) { recordingError = error }
        }
        
        mockRecorder.addRecordingStateListener(listener)
        
        // Test successful recording flow
        assertFalse(mockRecorder.isRecording())
        
        val startResult = mockRecorder.startRecording("/tmp/test.wav")
        assertTrue(startResult is AudioRecordingResult.Success)
        assertTrue(mockRecorder.isRecording())
        assertTrue(recordingStarted)
        
        val audioFile = mockRecorder.stopRecording()
        assertFalse(mockRecorder.isRecording())
        assertTrue(recordingStopped)
        assertNotNull(audioFile)
        assertEquals("/tmp/test.wav", audioFile.path)
        assertEquals(5000L, audioFile.durationMs)
        assertEquals(160000L, audioFile.sizeBytes)
        
        mockRecorder.removeRecordingStateListener(listener)
    }
    
    @Test
    fun testRecorderErrorHandling() = runTest {
        val mockRecorder = MockAudioRecorder()
        var errorReceived: AudioRecordingError? = null
        
        val listener = object : RecordingStateListener {
            override fun onRecordingStarted() {}
            override fun onRecordingPaused() {}
            override fun onRecordingResumed() {}
            override fun onRecordingStopped() {}
            override fun onRecordingError(error: AudioRecordingError) { errorReceived = error }
        }
        
        mockRecorder.addRecordingStateListener(listener)
        
        // Test start failure
        mockRecorder.setShouldFailStart(true)
        mockRecorder.setMockError(AudioRecordingError.PermissionDenied())
        
        val result = mockRecorder.startRecording("/tmp/test.wav")
        assertTrue(result is AudioRecordingResult.Error)
        assertTrue(result.error is AudioRecordingError.PermissionDenied)
        assertFalse(mockRecorder.isRecording())
        
        // Test double start
        mockRecorder.setShouldFailStart(false)
        mockRecorder.startRecording("/tmp/test.wav")
        val doubleStartResult = mockRecorder.startRecording("/tmp/test2.wav")
        assertTrue(doubleStartResult is AudioRecordingResult.Error)
        assertTrue(doubleStartResult.error is AudioRecordingError.ConfigurationError)
        
        // Test audio focus loss simulation
        mockRecorder.simulateAudioFocusLoss()
        assertFalse(mockRecorder.isRecording())
        assertNotNull(errorReceived)
        assertTrue(errorReceived is AudioRecordingError.DeviceUnavailable)
    }
    
    @Test
    fun testAudioFocusManager() = runTest {
        val mockFocusManager = MockAudioFocusManager()
        var lastFocusChange: AudioFocusChange? = null
        
        val listener = object : AudioFocusChangeListener {
            override fun onAudioFocusChange(focusChange: AudioFocusChange) {
                lastFocusChange = focusChange
            }
        }
        
        // Test successful focus request
        assertTrue(mockFocusManager.requestAudioFocus(listener))
        
        // Test focus change simulation
        mockFocusManager.simulateAudioFocusChange(AudioFocusChange.LOSS)
        assertEquals(AudioFocusChange.LOSS, lastFocusChange)
        
        mockFocusManager.simulateAudioFocusChange(AudioFocusChange.GAIN)
        assertEquals(AudioFocusChange.GAIN, lastFocusChange)
        
        // Test focus abandonment
        mockFocusManager.abandonAudioFocus()
        
        // Test failed focus request
        mockFocusManager.setShouldGrantFocus(false)
        assertFalse(mockFocusManager.requestAudioFocus(listener))
    }
    
    @Test
    fun testRecorderCancellation() = runTest {
        val mockRecorder = MockAudioRecorder()
        var recordingStopped = false
        
        val listener = object : RecordingStateListener {
            override fun onRecordingStarted() {}
            override fun onRecordingPaused() {}
            override fun onRecordingResumed() {}
            override fun onRecordingStopped() { recordingStopped = true }
            override fun onRecordingError(error: AudioRecordingError) {}
        }
        
        mockRecorder.addRecordingStateListener(listener)
        
        // Start recording
        mockRecorder.startRecording("/tmp/test.wav")
        assertTrue(mockRecorder.isRecording())
        
        // Cancel recording
        mockRecorder.cancelRecording()
        assertFalse(mockRecorder.isRecording())
        assertTrue(recordingStopped)
        
        // Cancel when not recording should not crash
        mockRecorder.cancelRecording()
    }
    
    @Test
    fun testRecorderPauseResume() = runTest {
        val mockRecorder = MockAudioRecorder()
        var paused = false
        var resumed = false
        
        val listener = object : RecordingStateListener {
            override fun onRecordingStarted() {}
            override fun onRecordingPaused() { paused = true }
            override fun onRecordingResumed() { resumed = true }
            override fun onRecordingStopped() {}
            override fun onRecordingError(error: AudioRecordingError) {}
        }
        
        mockRecorder.addRecordingStateListener(listener)
        mockRecorder.startRecording("/tmp/test.wav")
        
        // Simulate pause/resume
        mockRecorder.simulatePause()
        assertTrue(paused)
        
        mockRecorder.simulateResume()
        assertTrue(resumed)
    }
}