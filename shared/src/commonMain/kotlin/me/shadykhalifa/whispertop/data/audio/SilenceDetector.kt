package me.shadykhalifa.whispertop.data.audio

import me.shadykhalifa.whispertop.utils.TimeUtils

class SilenceDetector(
    private val silenceDurationThresholdMs: Long = RecordingConstraints.SILENCE_DURATION_MS,
    private val sampleRateHz: Int = RecordingConstraints.SAMPLE_RATE,
    private val bufferDurationMs: Long = RecordingConstraints.AUDIO_BUFFER_DURATION_MS
) {
    private var consecutiveSilentBuffers = 0
    private var consecutiveNonSilentBuffers = 0
    private var isCurrentlySilent = false
    private var silenceStartTime: Long? = null
    private var lastSilenceEndTime: Long? = null
    
    private val buffersForSilenceThreshold = (silenceDurationThresholdMs / bufferDurationMs).toInt()
    
    fun processSample(isSilent: Boolean): SilenceState {
        if (isSilent) {
            consecutiveSilentBuffers++
            consecutiveNonSilentBuffers = 0
            
            if (!isCurrentlySilent && consecutiveSilentBuffers >= buffersForSilenceThreshold) {
                isCurrentlySilent = true
                silenceStartTime = TimeUtils.currentTimeMillis() - (consecutiveSilentBuffers * bufferDurationMs)
                return SilenceState.ENTERED_SILENCE
            }
        } else {
            consecutiveNonSilentBuffers++
            consecutiveSilentBuffers = 0
            
            if (isCurrentlySilent && consecutiveNonSilentBuffers >= 2) {
                isCurrentlySilent = false
                lastSilenceEndTime = TimeUtils.currentTimeMillis()
                silenceStartTime = null
                return SilenceState.EXITED_SILENCE
            }
        }
        
        return if (isCurrentlySilent) SilenceState.IN_SILENCE else SilenceState.NOT_SILENT
    }
    
    fun getCurrentSilenceDuration(): Long {
        return if (isCurrentlySilent && silenceStartTime != null) {
            TimeUtils.currentTimeMillis() - silenceStartTime!!
        } else {
            0L
        }
    }
    
    fun shouldTrimSilence(): Boolean {
        return isCurrentlySilent && getCurrentSilenceDuration() > silenceDurationThresholdMs * 2
    }
    
    fun reset() {
        consecutiveSilentBuffers = 0
        consecutiveNonSilentBuffers = 0
        isCurrentlySilent = false
        silenceStartTime = null
        lastSilenceEndTime = null
    }
    
    enum class SilenceState {
        NOT_SILENT,
        ENTERED_SILENCE,
        IN_SILENCE,
        EXITED_SILENCE
    }
}