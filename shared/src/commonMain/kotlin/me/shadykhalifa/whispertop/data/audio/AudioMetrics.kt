package me.shadykhalifa.whispertop.data.audio

import kotlin.math.log10
import kotlin.math.sqrt

data class AudioMetrics(
    val rmsLevel: Float,        // Root Mean Square level (0.0 - 1.0)
    val peakLevel: Float,        // Peak amplitude (0.0 - 1.0)
    val dbLevel: Float,          // Level in decibels
    val isClipping: Boolean,     // True if audio is clipping
    val isSilent: Boolean,       // True if audio is below silence threshold
    val noiseFloor: Float,       // Estimated noise floor in dB
    val signalToNoise: Float,   // SNR in dB
    val qualityScore: Int       // Overall quality score (0-100)
) {
    companion object {
        fun calculate(audioBuffer: ShortArray): AudioMetrics {
            if (audioBuffer.isEmpty()) {
                return empty()
            }
            
            var sumSquares = 0.0
            var peak = 0.0f
            var clippingCount = 0
            
            for (sample in audioBuffer) {
                val normalized = sample / 32768.0f
                val abs = kotlin.math.abs(normalized)
                
                sumSquares += normalized * normalized
                if (abs > peak) peak = abs
                if (abs >= RecordingConstraints.CLIPPING_THRESHOLD) clippingCount++
            }
            
            val rms = sqrt(sumSquares / audioBuffer.size).toFloat()
            val dbLevel = if (rms > 0) 20 * log10(rms) else -100f
            val isSilent = dbLevel < RecordingConstraints.SILENCE_THRESHOLD_DB
            val isClipping = clippingCount > audioBuffer.size * 0.01 // More than 1% samples clipping
            
            val noiseFloor = estimateNoiseFloor(audioBuffer)
            val snr = if (noiseFloor < dbLevel) dbLevel - noiseFloor else 0f
            
            val qualityScore = calculateQualityScore(
                rms = rms,
                peak = peak,
                snr = snr,
                isClipping = isClipping,
                isSilent = isSilent
            )
            
            return AudioMetrics(
                rmsLevel = rms,
                peakLevel = peak,
                dbLevel = dbLevel,
                isClipping = isClipping,
                isSilent = isSilent,
                noiseFloor = noiseFloor,
                signalToNoise = snr,
                qualityScore = qualityScore
            )
        }
        
        private fun estimateNoiseFloor(audioBuffer: ShortArray): Float {
            if (audioBuffer.size < 100) return RecordingConstraints.NOISE_FLOOR_DB
            
            val sortedAmplitudes = audioBuffer
                .map { kotlin.math.abs(it / 32768.0f) }
                .sorted()
            
            val percentile10 = sortedAmplitudes[sortedAmplitudes.size / 10]
            return if (percentile10 > 0) 20 * log10(percentile10) else RecordingConstraints.NOISE_FLOOR_DB
        }
        
        private fun calculateQualityScore(
            rms: Float,
            peak: Float,
            snr: Float,
            isClipping: Boolean,
            isSilent: Boolean
        ): Int {
            var score = 50
            
            // Penalize clipping heavily
            if (isClipping) score -= 30
            
            // Penalize silence
            if (isSilent) score -= 20
            
            // Reward good SNR (> 20dB is good)
            score += (snr.coerceIn(0f, 40f) * 0.75f).toInt()
            
            // Reward optimal RMS levels (0.1 - 0.5 is good range)
            if (rms in 0.1f..0.5f) score += 20
            else if (rms < 0.05f) score -= 10
            
            // Ensure headroom (peak should be < 0.9)
            if (peak < 0.9f) score += 10
            
            return score.coerceIn(0, 100)
        }
        
        fun empty() = AudioMetrics(
            rmsLevel = 0f,
            peakLevel = 0f,
            dbLevel = -100f,
            isClipping = false,
            isSilent = true,
            noiseFloor = RecordingConstraints.NOISE_FLOOR_DB,
            signalToNoise = 0f,
            qualityScore = 0
        )
    }
}

data class RecordingStatistics(
    val duration: Long,                // Duration in milliseconds
    val fileSize: Long,                // Current file size in bytes
    val estimatedFinalSize: Long,      // Estimated final size based on current rate
    val remainingTime: Long,           // Remaining time before hitting size limit
    val averageLevel: Float,           // Average audio level (0.0 - 1.0)
    val peakLevel: Float,              // Peak level encountered
    val silencePercentage: Float,      // Percentage of recording that is silence
    val clippingOccurrences: Int,      // Number of clipping events
    val overallQuality: Int           // Overall quality score (0-100)
)