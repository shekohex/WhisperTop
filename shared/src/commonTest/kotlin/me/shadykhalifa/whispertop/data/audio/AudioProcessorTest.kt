package me.shadykhalifa.whispertop.data.audio

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AudioProcessorTest {
    
    @Test
    fun testTrimSilence() {
        val processor = AudioProcessor()
        
        // Create buffer with silence at beginning and end
        val buffer = ShortArray(1000) { i ->
            when {
                i < 200 -> 0 // Leading silence
                i > 800 -> 0 // Trailing silence
                else -> (1000 * kotlin.math.sin(i * 0.1)).toInt().toShort()
            }
        }
        
        val trimmed = processor.trimSilence(buffer)
        
        // Should be shorter than original (removed leading and trailing silence)
        assertTrue(trimmed.size < buffer.size)
        
        // Should have removed significant amount of silence
        // Original has 200 leading + 200 trailing = 400 samples of silence
        // But we keep small buffers, so expect to remove at least 200 samples
        assertTrue(buffer.size - trimmed.size > 100)
    }
    
    @Test
    fun testNormalization() {
        val processor = AudioProcessor()
        
        // Create quiet buffer
        val buffer = ShortArray(1000) { (1000 * kotlin.math.sin(it * 0.1)).toInt().toShort() }
        
        val normalized = processor.normalize(buffer)
        
        // Find peak of normalized audio
        val maxAmplitude = normalized.maxOf { abs(it.toInt()) }
        
        // Should be normalized to ~90% of max
        assertTrue(maxAmplitude > 29000)
        assertTrue(maxAmplitude < 32768)
    }
    
    @Test
    fun testNoiseGate() {
        val processor = AudioProcessor()
        
        // Create buffer with very quiet noise
        val buffer = ShortArray(1000) { i ->
            if (i in 400..600) {
                // Signal in the middle
                (10000 * kotlin.math.sin(i * 0.1)).toInt().toShort()
            } else {
                // Very quiet noise
                kotlin.random.Random.nextInt(-50, 50).toShort()
            }
        }
        
        val gated = processor.applyNoiseGate(buffer)
        
        // Quiet parts should be even quieter
        for (i in 0..100) {
            assertTrue(abs(gated[i].toInt()) <= abs(buffer[i].toInt()))
        }
        
        // Signal part should be preserved
        for (i in 450..550) {
            assertTrue(abs(gated[i].toInt()) >= abs(buffer[i].toInt()) * 0.9)
        }
    }
    
    @Test
    fun testHighPassFilter() {
        val processor = AudioProcessor()
        
        // Create buffer with low frequency component
        val buffer = ShortArray(1000) { i ->
            val lowFreq = (5000 * kotlin.math.sin(i * 0.01)).toInt() // Low frequency
            val highFreq = (1000 * kotlin.math.sin(i * 0.5)).toInt() // High frequency
            (lowFreq + highFreq).coerceIn(-32768, 32767).toShort()
        }
        
        val filtered = processor.applyHighPassFilter(buffer, 80f)
        
        // High-pass filter should reduce low frequency content
        val originalEnergy = buffer.sumOf { (it * it).toLong() }
        val filteredEnergy = filtered.sumOf { (it * it).toLong() }
        
        assertTrue(filteredEnergy < originalEnergy)
    }
    
    @Test
    fun testDetectNoiseLevel() {
        val processor = AudioProcessor()
        
        // Create buffer with known noise floor
        val buffer = ShortArray(1000) { i ->
            val signal = if (i % 10 == 0) {
                (10000 * kotlin.math.sin(i * 0.1)).toInt()
            } else {
                kotlin.random.Random.nextInt(-100, 100)
            }
            signal.coerceIn(-32768, 32767).toShort()
        }
        
        val noiseLevel = processor.detectNoiseLevel(buffer)
        
        // Noise level should be detected and reasonable
        assertTrue(noiseLevel < 0f) // Should be negative dB
        assertTrue(noiseLevel > -80f) // But not too low
    }
    
    @Test
    fun testCalculateDynamicRange() {
        val processor = AudioProcessor()
        
        // Create buffer with varying dynamics
        val buffer = ShortArray(1000) { i ->
            when {
                i < 333 -> (100 * kotlin.math.sin(i * 0.1)).toInt().toShort() // Quiet
                i < 666 -> (5000 * kotlin.math.sin(i * 0.1)).toInt().toShort() // Medium
                else -> (20000 * kotlin.math.sin(i * 0.1)).toInt().toShort() // Loud
            }
        }
        
        val dynamicRange = processor.calculateDynamicRange(buffer)
        
        // Should have significant dynamic range
        assertTrue(dynamicRange > 10f)
    }
    
    @Test
    fun testFullProcessingPipeline() {
        val processor = AudioProcessor(QualityPreset.HIGH)
        
        // Create realistic audio buffer with silence, signal, and noise
        val buffer = ShortArray(2000) { i ->
            when {
                i < 300 -> 0 // Leading silence
                i > 1700 -> 0 // Trailing silence
                i in 800..1000 -> kotlin.random.Random.nextInt(-50, 50).toShort() // Noise section
                else -> (5000 * kotlin.math.sin(i * 0.1)).toInt().toShort() // Signal
            }
        }
        
        val processed = processor.processAudio(buffer)
        
        // Should be shorter due to silence trimming
        assertTrue(processed.size < buffer.size)
        
        // Should have good amplitude
        val maxAmplitude = processed.maxOf { abs(it.toInt()) }
        assertTrue(maxAmplitude > 10000)
    }
}