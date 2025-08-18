package me.shadykhalifa.whispertop.data.audio

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

class AudioProcessor(
    private val qualityPreset: QualityPreset = QualityPreset.MEDIUM,
    private val gainFactor: Float = 1.0f // Default no amplification for backward compatibility
) {
    
    fun processAudio(audioData: ShortArray): ShortArray {
        var processed = audioData
        
        // Apply gain amplification first to boost weak signals
        if (gainFactor != 1.0f) {
            processed = applyGain(processed)
        }
        
        if (qualityPreset.silenceTrimming) {
            processed = trimSilence(processed)
        }
        
        if (qualityPreset.noiseReduction) {
            processed = applyNoiseGate(processed)
        }
        
        if (qualityPreset.normalization) {
            processed = normalize(processed)
        }
        
        return processed
    }
    
    fun trimSilence(audioData: ShortArray): ShortArray {
        if (audioData.isEmpty()) return audioData
        
        val threshold = (32768 * 0.01f).toInt() // 1% of max amplitude
        
        // Find start of audio
        var startIndex = 0
        for (i in audioData.indices) {
            if (abs(audioData[i].toInt()) > threshold) {
                startIndex = max(0, i - 100) // Keep a small buffer before audio starts
                break
            }
        }
        
        // Find end of audio
        var endIndex = audioData.size - 1
        for (i in audioData.indices.reversed()) {
            if (abs(audioData[i].toInt()) > threshold) {
                endIndex = min(audioData.size - 1, i + 100) // Keep a small buffer after audio ends
                break
            }
        }
        
        return if (startIndex < endIndex) {
            audioData.sliceArray(startIndex..endIndex)
        } else {
            audioData
        }
    }
    
    fun applyNoiseGate(audioData: ShortArray): ShortArray {
        if (audioData.isEmpty()) return audioData
        
        val gateThreshold = (32768 * 0.001f).toInt() // 0.1% of max amplitude (less aggressive)
        val windowSize = 100
        val result = ShortArray(audioData.size)
        
        for (i in audioData.indices) {
            val windowStart = max(0, i - windowSize / 2)
            val windowEnd = min(audioData.size - 1, i + windowSize / 2)
            
            var windowEnergy = 0.0
            for (j in windowStart..windowEnd) {
                val sample = audioData[j] / 32768.0
                windowEnergy += sample * sample
            }
            windowEnergy = sqrt(windowEnergy / (windowEnd - windowStart + 1))
            
            result[i] = if (windowEnergy * 32768 > gateThreshold) {
                audioData[i]
            } else {
                (audioData[i] * 0.1f).toInt().toShort() // Reduce by 90% instead of complete silence
            }
        }
        
        return result
    }
    
    fun normalize(audioData: ShortArray): ShortArray {
        if (audioData.isEmpty()) return audioData
        
        // Find peak amplitude
        var maxAmplitude = 0
        for (sample in audioData) {
            val abs = abs(sample.toInt())
            if (abs > maxAmplitude) {
                maxAmplitude = abs
            }
        }
        
        if (maxAmplitude == 0) return audioData
        
        // Calculate normalization factor (normalize to 90% to avoid clipping)
        val targetAmplitude = (32767 * 0.9f).toInt()
        val normalizationFactor = targetAmplitude.toFloat() / maxAmplitude
        
        // Don't amplify if already loud enough
        if (normalizationFactor > 1.0f && maxAmplitude > 16384) {
            return audioData // Already loud enough
        }
        
        // Apply normalization
        val result = ShortArray(audioData.size)
        for (i in audioData.indices) {
            result[i] = (audioData[i] * normalizationFactor).roundToInt()
                .coerceIn(-32768, 32767).toShort()
        }
        
        return result
    }
    
    fun detectNoiseLevel(audioData: ShortArray): Float {
        if (audioData.isEmpty()) return RecordingConstraints.NOISE_FLOOR_DB
        
        // Sort samples by amplitude to find noise floor
        val sortedAmplitudes = audioData
            .map { abs(it.toInt()) / 32768.0f }
            .sorted()
        
        // Use 10th percentile as noise floor estimate
        val percentileIndex = (sortedAmplitudes.size * 0.1f).toInt()
        val noiseFloor = sortedAmplitudes[percentileIndex]
        
        return if (noiseFloor > 0) {
            (20 * kotlin.math.log10(noiseFloor)).toFloat()
        } else {
            RecordingConstraints.NOISE_FLOOR_DB
        }
    }
    
    fun calculateDynamicRange(audioData: ShortArray): Float {
        if (audioData.isEmpty()) return 0f
        
        val sortedAmplitudes = audioData
            .map { abs(it.toInt()) / 32768.0f }
            .filter { it > 0 }
            .sorted()
        
        if (sortedAmplitudes.isEmpty()) return 0f
        
        val percentile10 = sortedAmplitudes[(sortedAmplitudes.size * 0.1f).toInt()]
        val percentile90 = sortedAmplitudes[(sortedAmplitudes.size * 0.9f).toInt()]
        
        return if (percentile10 > 0 && percentile90 > 0) {
            (20 * kotlin.math.log10(percentile90 / percentile10)).toFloat()
        } else {
            0f
        }
    }
    
    fun applyHighPassFilter(audioData: ShortArray, cutoffFrequency: Float = 80f): ShortArray {
        if (audioData.isEmpty()) return audioData
        
        val sampleRate = RecordingConstraints.SAMPLE_RATE.toFloat()
        val rc = 1.0f / (2.0f * kotlin.math.PI * cutoffFrequency)
        val dt = 1.0f / sampleRate
        val alpha = rc / (rc + dt)
        
        val result = ShortArray(audioData.size)
        var previousInput = 0f
        var previousOutput = 0f
        
        for (i in audioData.indices) {
            val currentInput = audioData[i] / 32768.0f
            val currentOutput = alpha * (previousOutput + currentInput - previousInput)
            
            result[i] = (currentOutput * 32768).roundToInt()
                .coerceIn(-32768, 32767).toShort()
            
            previousInput = currentInput
            previousOutput = currentOutput.toFloat()
        }
        
        return result
    }
    
    fun applyGain(audioData: ShortArray): ShortArray {
        if (audioData.isEmpty() || gainFactor == 1.0f) return audioData
        
        val result = ShortArray(audioData.size)
        
        for (i in audioData.indices) {
            val amplified = (audioData[i] * gainFactor).roundToInt()
            // Clip to prevent overflow and distortion
            result[i] = amplified.coerceIn(-32768, 32767).toShort()
        }
        
        return result
    }
    
    fun calculateAutoGain(audioData: ShortArray): Float {
        if (audioData.isEmpty()) return 1.0f
        
        // Calculate RMS (Root Mean Square) for average signal level
        var sumOfSquares = 0.0
        for (sample in audioData) {
            val normalized = sample / 32768.0
            sumOfSquares += normalized * normalized
        }
        val rms = sqrt(sumOfSquares / audioData.size)
        
        // Target RMS level (about 25% of maximum)
        val targetRms = 0.25
        
        // Calculate gain needed to reach target
        val suggestedGain = if (rms > 0) {
            (targetRms / rms).toFloat()
        } else {
            1.0f
        }
        
        // Limit gain to reasonable range (0.5x to 8x)
        return suggestedGain.coerceIn(0.5f, 8.0f)
    }
}