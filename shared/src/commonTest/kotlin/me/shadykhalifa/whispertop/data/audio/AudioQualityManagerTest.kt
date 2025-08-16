package me.shadykhalifa.whispertop.data.audio

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AudioQualityManagerTest {
    
    @Test
    fun testInitialState() = runTest {
        val manager = AudioQualityManager()
        
        val metrics = manager.currentMetrics.value
        assertEquals(0f, metrics.rmsLevel)
        assertEquals(0f, metrics.peakLevel)
        assertEquals(-100f, metrics.dbLevel)
        assertFalse(metrics.isClipping)
        assertTrue(metrics.isSilent)
        
        val stats = manager.recordingStatistics.value
        assertEquals(null, stats)
    }
    
    @Test
    fun testStartMonitoring() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process some audio
        val buffer = ShortArray(1000) { (1000 * kotlin.math.sin(it * 0.1)).toInt().toShort() }
        manager.processAudioBuffer(buffer)
        
        val metrics = manager.currentMetrics.value
        assertTrue(metrics.rmsLevel > 0f)
        assertFalse(metrics.isSilent)
        
        val stats = manager.recordingStatistics.value
        assertNotNull(stats)
        assertTrue(stats.fileSize > 0)
    }
    
    @Test
    fun testFileSizeTracking() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process multiple buffers
        val buffer = ShortArray(1000) { 1000 }
        repeat(10) {
            manager.processAudioBuffer(buffer)
        }
        
        val stats = manager.recordingStatistics.value
        assertNotNull(stats)
        assertEquals(20000L, stats.fileSize) // 10 buffers * 1000 samples * 2 bytes
        assertTrue(stats.estimatedFinalSize > 0)
        assertTrue(stats.remainingTime > 0)
    }
    
    @Test
    fun testShouldStopRecording() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process a buffer first to initialize statistics
        val normalBuffer = ShortArray(1000) { 1000 }
        manager.processAudioBuffer(normalBuffer)
        
        // Normal case - should not stop with small amount of data
        assertFalse(manager.shouldStopRecording())
        
        // Check that statistics are properly initialized
        val stats = manager.recordingStatistics.value
        if (stats != null) {
            // Verify that with normal recording, we don't stop
            assertTrue(stats.estimatedFinalSize < RecordingConstraints.MAX_FILE_SIZE_BYTES * 0.95)
        }
        
        // Verify shouldStopRecording returns false for normal recording
        assertFalse(manager.shouldStopRecording())
    }
    
    @Test
    fun testCanContinueRecording() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        val buffer = ShortArray(1000) { 1000 }
        manager.processAudioBuffer(buffer)
        
        // Should be able to continue for a reasonable duration
        assertTrue(manager.canContinueRecording(10000)) // 10 seconds
        
        // Simulate near limit
        val largeBuffer = ShortArray(12 * 1024 * 1024) { 1000 }
        manager.processAudioBuffer(largeBuffer)
        assertFalse(manager.canContinueRecording(60000)) // 60 seconds
    }
    
    @Test
    fun testQualityReport() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process various quality buffers
        val goodBuffer = ShortArray(1000) { (8000 * kotlin.math.sin(it * 0.1)).toInt().toShort() }
        val quietBuffer = ShortArray(1000) { 50 }
        val clippingBuffer = ShortArray(1000) { 32767 }
        
        manager.processAudioBuffer(goodBuffer)
        manager.processAudioBuffer(quietBuffer)
        manager.processAudioBuffer(clippingBuffer)
        
        val report = manager.getQualityReport()
        
        assertNotNull(report)
        assertTrue(report.overallQuality in 0..100)
        assertNotNull(report.audioMetrics)
        assertNotNull(report.recordingStatistics)
        assertTrue(report.issues.isNotEmpty()) // Should detect clipping
        assertTrue(report.recommendations.isNotEmpty())
    }
    
    @Test
    fun testClippingDetection() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process buffer with clipping
        val clippingBuffer = ShortArray(1000) { if (it % 10 == 0) 32767 else 16000 }
        manager.processAudioBuffer(clippingBuffer)
        
        val metrics = manager.currentMetrics.value
        assertTrue(metrics.isClipping)
        
        val report = manager.getQualityReport()
        assertTrue(QualityIssue.CLIPPING in report.issues)
        assertTrue(report.recommendations.any { it.contains("gain") || it.contains("microphone") })
    }
    
    @Test
    fun testSilenceDetection() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process silent buffers
        val silentBuffer = ShortArray(1000) { 0 }
        repeat(10) {
            manager.processAudioBuffer(silentBuffer)
        }
        
        val stats = manager.recordingStatistics.value
        assertNotNull(stats)
        assertEquals(100f, stats.silencePercentage)
        
        val report = manager.getQualityReport()
        assertTrue(QualityIssue.TOO_MUCH_SILENCE in report.issues)
    }
    
    @Test
    fun testMetricsHistory() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        // Process many buffers to test history management
        repeat(150) { i ->
            val buffer = ShortArray(100) { (1000 * kotlin.math.sin(i * 0.1)).toInt().toShort() }
            manager.processAudioBuffer(buffer)
        }
        
        // Should maintain reasonable history (max 100 entries as per implementation)
        val report = manager.getQualityReport()
        assertNotNull(report)
        assertTrue(report.overallQuality > 0)
    }
    
    @Test
    fun testRemainingTime() = runTest {
        val manager = AudioQualityManager()
        manager.startMonitoring()
        
        val initialRemaining = manager.getRemainingRecordingTime()
        assertTrue(initialRemaining > 0)
        assertEquals(RecordingConstraints.MAX_RECORDING_DURATION_MS, initialRemaining)
        
        // Process some audio and wait a bit for meaningful duration
        val buffer = ShortArray(16000) { 1000 } // 1 second at 16kHz
        manager.processAudioBuffer(buffer)
        
        // Wait to get meaningful duration for calculations
        Thread.sleep(150)
        
        // Process another buffer to trigger recalculation with meaningful duration
        manager.processAudioBuffer(buffer)
        
        val newRemaining = manager.getRemainingRecordingTime()
        // Should have remaining time, and it should be positive
        assertTrue(newRemaining > 0)
        // With data processed, remaining time should be less than max
        assertTrue(newRemaining <= RecordingConstraints.MAX_RECORDING_DURATION_MS)
    }
}