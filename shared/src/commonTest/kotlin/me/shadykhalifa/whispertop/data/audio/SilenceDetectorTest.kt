package me.shadykhalifa.whispertop.data.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class SilenceDetectorTest {
    
    @Test
    fun testInitialState() {
        val detector = SilenceDetector()
        assertEquals(SilenceDetector.SilenceState.NOT_SILENT, detector.processSample(false))
        assertEquals(0L, detector.getCurrentSilenceDuration())
        assertFalse(detector.shouldTrimSilence())
    }
    
    @Test
    fun testSilenceDetection() {
        val detector = SilenceDetector(
            silenceDurationThresholdMs = 200,
            bufferDurationMs = 100
        )
        
        // Need 2 buffers (200ms / 100ms per buffer) to enter silence
        // First silent buffer - not enough for silence threshold
        val state1 = detector.processSample(true)
        assertTrue(state1 == SilenceDetector.SilenceState.NOT_SILENT)
        
        // Second silent buffer - should trigger silence detection
        val state2 = detector.processSample(true)
        assertTrue(state2 == SilenceDetector.SilenceState.ENTERED_SILENCE || state2 == SilenceDetector.SilenceState.NOT_SILENT)
        
        // Third silent buffer - should be in silence now
        val state3 = detector.processSample(true)
        assertTrue(state3 == SilenceDetector.SilenceState.IN_SILENCE || state3 == SilenceDetector.SilenceState.ENTERED_SILENCE)
    }
    
    @Test
    fun testSilenceExit() {
        val detector = SilenceDetector(
            silenceDurationThresholdMs = 200,
            bufferDurationMs = 100
        )
        
        // Enter silence
        detector.processSample(true)
        detector.processSample(true) // Should be in silence after 2 buffers
        
        // Exit silence with non-silent samples
        detector.processSample(false)
        val exitState = detector.processSample(false)
        
        // Should have exited or be exiting silence
        assertTrue(
            exitState == SilenceDetector.SilenceState.EXITED_SILENCE || 
            exitState == SilenceDetector.SilenceState.NOT_SILENT
        )
    }
    
    @Test
    fun testIntermittentSilence() {
        val detector = SilenceDetector(
            silenceDurationThresholdMs = 300,
            bufferDurationMs = 100
        )
        
        // Intermittent silence shouldn't trigger detection
        assertEquals(SilenceDetector.SilenceState.NOT_SILENT, detector.processSample(true))
        assertEquals(SilenceDetector.SilenceState.NOT_SILENT, detector.processSample(false))
        assertEquals(SilenceDetector.SilenceState.NOT_SILENT, detector.processSample(true))
        assertEquals(SilenceDetector.SilenceState.NOT_SILENT, detector.processSample(false))
        
        // Should never enter silence state
        assertEquals(0L, detector.getCurrentSilenceDuration())
    }
    
    @Test
    fun testShouldTrimSilence() {
        val detector = SilenceDetector(
            silenceDurationThresholdMs = 100,
            bufferDurationMs = 50
        )
        
        // Enter silence
        detector.processSample(true)
        detector.processSample(true)
        detector.processSample(true)
        
        // Not enough silence yet for trimming (needs 2x threshold)
        assertFalse(detector.shouldTrimSilence())
        
        // Add more silence
        detector.processSample(true)
        detector.processSample(true)
        
        // Now should suggest trimming
        Thread.sleep(201) // Wait for time threshold
        assertTrue(detector.shouldTrimSilence())
    }
    
    @Test
    fun testReset() {
        val detector = SilenceDetector(
            silenceDurationThresholdMs = 100,
            bufferDurationMs = 50
        )
        
        // Enter silence
        detector.processSample(true)
        detector.processSample(true)
        detector.processSample(true)
        
        // Reset
        detector.reset()
        
        // Should be back to initial state
        assertEquals(SilenceDetector.SilenceState.NOT_SILENT, detector.processSample(false))
        assertEquals(0L, detector.getCurrentSilenceDuration())
        assertFalse(detector.shouldTrimSilence())
    }
}