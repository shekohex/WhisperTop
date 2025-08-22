package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents audio format configuration for recording and processing
 */
@Serializable
data class AudioFormat(
    val sampleRate: Int = 16000,
    val channelCount: Int = 1,
    val bitDepth: Int = 16
) {
    /**
     * Calculate bytes per second based on format
     */
    val bytesPerSecond: Int get() = sampleRate * channelCount * (bitDepth / 8)
    
    /**
     * Calculate bytes per frame
     */
    val bytesPerFrame: Int get() = channelCount * (bitDepth / 8)
    
    /**
     * Check if format is valid for WhisperAPI
     */
    val isValidForWhisper: Boolean get() = 
        sampleRate in 8000..48000 && channelCount == 1 && bitDepth == 16
    
    companion object {
        /**
         * Default format for Whisper API (16kHz, mono, 16-bit)
         */
        val WHISPER_DEFAULT = AudioFormat(
            sampleRate = 16000,
            channelCount = 1,
            bitDepth = 16
        )
        
        /**
         * High quality format (44.1kHz, mono, 16-bit)
         */
        val HIGH_QUALITY = AudioFormat(
            sampleRate = 44100,
            channelCount = 1,
            bitDepth = 16
        )
        
        /**
         * Low quality format for testing (8kHz, mono, 16-bit)
         */
        val LOW_QUALITY = AudioFormat(
            sampleRate = 8000,
            channelCount = 1,
            bitDepth = 16
        )
    }
}