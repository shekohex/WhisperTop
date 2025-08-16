package me.shadykhalifa.whispertop.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RecordingStateTest {
    
    @Test
    fun testIdleState() {
        val state = RecordingState.Idle
        assertTrue(state is RecordingState.Idle)
        assertEquals(RecordingState.Idle, state)
    }
    
    @Test
    fun testRecordingState() {
        val startTime = 1000L
        val duration = 5000L
        val state = RecordingState.Recording(startTime = startTime, duration = duration)
        
        assertTrue(state is RecordingState.Recording)
        assertEquals(startTime, state.startTime)
        assertEquals(duration, state.duration)
    }
    
    @Test
    fun testRecordingStateWithDefaultDuration() {
        val startTime = 1000L
        val state = RecordingState.Recording(startTime = startTime)
        
        assertTrue(state is RecordingState.Recording)
        assertEquals(startTime, state.startTime)
        assertEquals(0L, state.duration)
    }
    
    @Test
    fun testProcessingState() {
        val progress = 0.5f
        val state = RecordingState.Processing(progress = progress)
        
        assertTrue(state is RecordingState.Processing)
        assertEquals(progress, state.progress)
    }
    
    @Test
    fun testProcessingStateWithDefaultProgress() {
        val state = RecordingState.Processing()
        
        assertTrue(state is RecordingState.Processing)
        assertEquals(0f, state.progress)
    }
    
    @Test
    fun testSuccessState() {
        val audioFile = AudioFile(
            path = "/test/path.wav",
            durationMs = 5000L,
            sizeBytes = 1024L
        )
        val transcription = "Test transcription"
        val state = RecordingState.Success(audioFile = audioFile, transcription = transcription)
        
        assertTrue(state is RecordingState.Success)
        assertEquals(audioFile, state.audioFile)
        assertEquals(transcription, state.transcription)
    }
    
    @Test
    fun testErrorState() {
        val throwable = RuntimeException("Test error")
        val retryable = true
        val state = RecordingState.Error(throwable = throwable, retryable = retryable)
        
        assertTrue(state is RecordingState.Error)
        assertEquals(throwable, state.throwable)
        assertEquals(retryable, state.retryable)
    }
    
    @Test
    fun testErrorStateWithDefaultRetryable() {
        val throwable = RuntimeException("Test error")
        val state = RecordingState.Error(throwable = throwable)
        
        assertTrue(state is RecordingState.Error)
        assertEquals(throwable, state.throwable)
        assertTrue(state.retryable)
    }
    
    @Test
    fun testStateEquality() {
        val startTime = 1000L
        val duration = 5000L
        
        val recording1 = RecordingState.Recording(startTime = startTime, duration = duration)
        val recording2 = RecordingState.Recording(startTime = startTime, duration = duration)
        val recording3 = RecordingState.Recording(startTime = startTime, duration = 3000L)
        
        assertEquals(recording1, recording2)
        assertNotEquals(recording1, recording3)
        assertNotEquals<RecordingState>(recording1, RecordingState.Idle)
    }
    
    @Test
    fun testStateCopy() {
        val original = RecordingState.Recording(startTime = 1000L, duration = 0L)
        val updated = original.copy(duration = 5000L)
        
        assertEquals(1000L, updated.startTime)
        assertEquals(5000L, updated.duration)
        assertNotEquals(original, updated)
    }
    
    @Test
    fun testSealedClassHierarchy() {
        val states = listOf(
            RecordingState.Idle,
            RecordingState.Recording(startTime = 1000L),
            RecordingState.Processing(progress = 0.5f),
            RecordingState.Success(
                audioFile = AudioFile("/test.wav", 1000L, 512L),
                transcription = "Test"
            ),
            RecordingState.Error(throwable = RuntimeException("Test"))
        )
        
        states.forEach { state ->
            assertTrue(state is RecordingState, "State should be instance of RecordingState")
        }
        
        // Test when statements work correctly
        states.forEach { state ->
            val result = when (state) {
                is RecordingState.Idle -> "idle"
                is RecordingState.Recording -> "recording"
                is RecordingState.Processing -> "processing"
                is RecordingState.Success -> "success"
                is RecordingState.Error -> "error"
            }
            assertTrue(result.isNotEmpty(), "Result should not be empty")
        }
    }
    
    @Test
    fun testErrorStateRetryableFlag() {
        val retryableError = RecordingState.Error(
            throwable = RuntimeException("Network error"),
            retryable = true
        )
        val nonRetryableError = RecordingState.Error(
            throwable = SecurityException("Permission denied"),
            retryable = false
        )
        
        assertTrue(retryableError.retryable)
        assertFalse(nonRetryableError.retryable)
    }
}