package me.shadykhalifa.whispertop.data.audio

object RecordingConstraints {
    const val MAX_FILE_SIZE_BYTES = 25 * 1024 * 1024L // 25MB OpenAI limit
    const val SAMPLE_RATE = 16000
    const val BITS_PER_SAMPLE = 16
    const val CHANNELS = 1 // Mono
    
    const val BYTES_PER_SECOND = (SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8)
    const val MAX_RECORDING_DURATION_MS = (MAX_FILE_SIZE_BYTES * 1000L) / BYTES_PER_SECOND
    
    const val SILENCE_THRESHOLD_DB = -40.0f
    const val SILENCE_DURATION_MS = 2000L
    
    const val NOISE_FLOOR_DB = -50.0f
    const val CLIPPING_THRESHOLD = 0.99f
    
    const val MIN_RECORDING_DURATION_MS = 100L
    const val AUDIO_BUFFER_DURATION_MS = 100L
}

enum class AudioQuality {
    LOW,    // Lower quality, smaller file size
    MEDIUM, // Balanced quality and size
    HIGH    // Best quality, larger file size
}

data class QualityPreset(
    val quality: AudioQuality,
    val sampleRate: Int,
    val bitDepth: Int,
    val noiseReduction: Boolean,
    val silenceTrimming: Boolean,
    val normalization: Boolean
) {
    companion object {
        val LOW = QualityPreset(
            quality = AudioQuality.LOW,
            sampleRate = 16000,
            bitDepth = 16,
            noiseReduction = false,
            silenceTrimming = true,
            normalization = false
        )
        
        val MEDIUM = QualityPreset(
            quality = AudioQuality.MEDIUM,
            sampleRate = 16000,
            bitDepth = 16,
            noiseReduction = false,
            silenceTrimming = true,
            normalization = true
        )
        
        val HIGH = QualityPreset(
            quality = AudioQuality.HIGH,
            sampleRate = 16000,
            bitDepth = 16,
            noiseReduction = true,
            silenceTrimming = true,
            normalization = true
        )
        
        fun getPreset(quality: AudioQuality): QualityPreset {
            return when (quality) {
                AudioQuality.LOW -> LOW
                AudioQuality.MEDIUM -> MEDIUM
                AudioQuality.HIGH -> HIGH
            }
        }
    }
}