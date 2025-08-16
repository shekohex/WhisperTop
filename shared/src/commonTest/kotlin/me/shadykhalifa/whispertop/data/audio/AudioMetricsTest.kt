package me.shadykhalifa.whispertop.data.audio

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class AudioMetricsTest {
    
    @Test
    fun testEmptyBuffer() {
        val metrics = AudioMetrics.calculate(shortArrayOf())
        assertEquals(0f, metrics.rmsLevel)
        assertEquals(0f, metrics.peakLevel)
        assertEquals(-100f, metrics.dbLevel)
        assertFalse(metrics.isClipping)
        assertTrue(metrics.isSilent)
        assertEquals(0, metrics.qualityScore)
    }
    
    @Test
    fun testSilentBuffer() {
        val buffer = ShortArray(1000) { 0 }
        val metrics = AudioMetrics.calculate(buffer)
        
        assertEquals(0f, metrics.rmsLevel)
        assertEquals(0f, metrics.peakLevel)
        assertEquals(-100f, metrics.dbLevel)
        assertFalse(metrics.isClipping)
        assertTrue(metrics.isSilent)
        assertTrue(metrics.qualityScore >= 0) // Just check it's non-negative
    }
    
    @Test
    fun testNormalAudioBuffer() {
        // Create a buffer with moderate amplitude (50% of max)
        val buffer = ShortArray(1000) { (16384 * kotlin.math.sin(it * 0.1)).toInt().toShort() }
        val metrics = AudioMetrics.calculate(buffer)
        
        assertTrue(metrics.rmsLevel > 0f)
        assertTrue(metrics.peakLevel > 0f)
        assertTrue(metrics.dbLevel > -40f)
        assertFalse(metrics.isClipping)
        assertFalse(metrics.isSilent)
        assertTrue(metrics.qualityScore > 30)
    }
    
    @Test
    fun testClippingDetection() {
        // Create a buffer with clipping values
        val buffer = ShortArray(1000) { if (it % 10 == 0) 32767 else 16384 }
        val metrics = AudioMetrics.calculate(buffer)
        
        assertTrue(metrics.isClipping)
        assertTrue(metrics.peakLevel >= RecordingConstraints.CLIPPING_THRESHOLD)
        assertTrue(metrics.qualityScore < 50) // Quality should be reduced due to clipping
    }
    
    @Test
    fun testQuietAudioDetection() {
        // Create a very quiet buffer
        val buffer = ShortArray(1000) { (100 * kotlin.math.sin(it * 0.1)).toInt().toShort() }
        val metrics = AudioMetrics.calculate(buffer)
        
        assertTrue(metrics.rmsLevel < 0.01f)
        assertTrue(metrics.dbLevel < -30f)
        assertFalse(metrics.isClipping)
    }
    
    @Test
    fun testQualityScoreRange() {
        // Test various buffers to ensure quality score stays in range
        val buffers = listOf(
            ShortArray(1000) { 0 }, // Silent
            ShortArray(1000) { 100 }, // Very quiet
            ShortArray(1000) { 16384 }, // Normal
            ShortArray(1000) { 32767 } // Clipping
        )
        
        for (buffer in buffers) {
            val metrics = AudioMetrics.calculate(buffer)
            assertTrue(metrics.qualityScore in 0..100)
        }
    }
    
    @Test
    fun testSignalToNoiseRatio() {
        // Create buffer with signal and noise
        val buffer = ShortArray(1000) { i ->
            val signal = (16384 * kotlin.math.sin(i * 0.1)).toInt()
            val noise = (kotlin.random.Random.nextInt(-1000, 1000))
            (signal + noise).coerceIn(-32768, 32767).toShort()
        }
        
        val metrics = AudioMetrics.calculate(buffer)
        assertTrue(metrics.signalToNoise > 0f)
        assertTrue(metrics.noiseFloor < metrics.dbLevel)
    }
}