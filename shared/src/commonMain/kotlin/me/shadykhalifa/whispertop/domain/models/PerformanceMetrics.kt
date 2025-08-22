package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class RecordingMetrics(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val audioFileSize: Long = 0,
    val sampleRate: Int = 0,
    val channels: Int = 0,
    val bitRate: Int = 0,
    val pauseCount: Int = 0,
    val totalPauseTime: Long = 0,
    val peakMemoryUsage: Long = 0,
    val averageMemoryUsage: Long = 0,
    val bufferUnderrunCount: Int = 0,
    val success: Boolean = false,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val deviceInfo: DeviceInfo? = null
) {
    fun getTotalRecordingTime(): Long = duration - totalPauseTime
    
    fun getEfficiencyRatio(): Float = if (duration > 0) getTotalRecordingTime().toFloat() / duration else 0f
    
    fun getMemoryEfficiency(): Float = if (peakMemoryUsage > 0) averageMemoryUsage.toFloat() / peakMemoryUsage else 0f
}

@Serializable
data class TranscriptionMetrics(
    val sessionId: String,
    val requestStartTime: Long,
    val requestEndTime: Long? = null,
    val apiCallDuration: Long = 0,
    val networkRequestSize: Long = 0,
    val networkResponseSize: Long = 0,
    val retryCount: Int = 0,
    val transcriptionLength: Int = 0,
    val audioFileDuration: Long = 0,
    val audioFileSize: Long = 0,
    val model: String = "",
    val language: String? = null,
    val detectedLanguage: String? = null,
    val success: Boolean = false,
    val errorType: String? = null,
    val errorMessage: String? = null,
    val httpStatusCode: Int? = null,
    val connectionTimeMs: Long = 0,
    val dnsLookupTimeMs: Long = 0,
    val sslHandshakeTimeMs: Long = 0,
    val transferTimeMs: Long = 0
) {
    fun getTranscriptionSpeed(): Float = 
        if (apiCallDuration > 0) (transcriptionLength.toFloat() / apiCallDuration) * 1000 else 0f
    
    fun getCompressionRatio(): Float = 
        if (networkRequestSize > 0) audioFileSize.toFloat() / networkRequestSize else 0f
    
    fun getNetworkEfficiency(): Float = 
        if (apiCallDuration > 0) transferTimeMs.toFloat() / apiCallDuration else 0f
}

@Serializable
data class DeviceInfo(
    val manufacturer: String = "",
    val model: String = "",
    val osVersion: String = "",
    val appVersion: String = "",
    val availableMemory: Long = 0,
    val totalMemory: Long = 0,
    val cpuCoreCount: Int = 0,
    val architecture: String = ""
)

@Serializable
data class PerformanceSession(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val recordingMetrics: RecordingMetrics? = null,
    val transcriptionMetrics: TranscriptionMetrics? = null,
    val memorySnapshots: List<MemorySnapshot> = emptyList(),
    val performanceWarnings: List<PerformanceWarning> = emptyList()
) {
    fun getTotalSessionTime(): Long = endTime?.let { it - startTime } ?: 0
    
    fun getOverallSuccess(): Boolean = 
        recordingMetrics?.success == true && transcriptionMetrics?.success == true
}

@Serializable
data class MemorySnapshot(
    val timestamp: Long,
    val usedMemory: Long,
    val freeMemory: Long,
    val totalMemory: Long,
    val heapSize: Long,
    val context: String = ""
) {
    fun getUsagePercentage(): Float = if (totalMemory > 0) (usedMemory.toFloat() / totalMemory) * 100 else 0f
}

@Serializable
data class PerformanceWarning(
    val timestamp: Long,
    val type: WarningType,
    val message: String,
    val severity: WarningSeverity,
    val context: String = "",
    val metrics: Map<String, String> = emptyMap()
)

enum class WarningType {
    HIGH_MEMORY_USAGE,
    SLOW_TRANSCRIPTION,
    LONG_RECORDING_SESSION,
    NETWORK_LATENCY,
    API_ERROR_RATE,
    BUFFER_UNDERRUN,
    LOW_STORAGE_SPACE,
    BATTERY_LOW,
    THERMAL_THROTTLING
}

enum class WarningSeverity {
    INFO,
    WARNING,
    CRITICAL
}

@Serializable
data class PerformanceThresholds(
    val maxRecordingDurationMs: Long = 300_000, // 5 minutes
    val maxTranscriptionTimeMs: Long = 10_000, // 10 seconds
    val maxMemoryUsagePercent: Float = 80f,
    val maxApiRetryCount: Int = 3,
    val minNetworkSpeedKbps: Float = 100f,
    val maxBufferUnderruns: Int = 5,
    val warningMemoryUsagePercent: Float = 70f,
    val criticalMemoryUsagePercent: Float = 90f
)

@Serializable
data class SessionMetrics(
    val sessionId: String,
    val sessionStartTime: Long,
    val sessionEndTime: Long? = null,
    val audioRecordingDuration: Long = 0,
    val audioFileSize: Long = 0,
    val audioQuality: String? = null,
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val speakingRate: Double = 0.0,
    val transcriptionText: String? = null,
    val transcriptionSuccess: Boolean = false,
    val textInsertionSuccess: Boolean = false,
    val targetAppPackage: String? = null,
    val errorType: String? = null,
    val errorMessage: String? = null
) {
    companion object {
        const val MAX_WORD_COUNT = 10000  // Max words per session
        const val MAX_CHARACTER_COUNT = 50000  // Max characters per session
        const val MAX_SPEAKING_RATE = 1000.0  // Max WPM (extremely fast speakers ~400 WPM)
        const val MIN_SPEAKING_RATE = 0.1  // Min WPM (very slow or pause-heavy speech)
        const val MAX_AUDIO_DURATION_MS = 600_000L  // 10 minutes max recording
        const val MAX_AUDIO_FILE_SIZE = 50_000_000L  // 50MB max file size
        const val MAX_TRANSCRIPTION_LENGTH = 20_000  // Max transcription text length
    }
    
    fun getSessionDuration(): Long = sessionEndTime?.let { it - sessionStartTime } ?: 0
    
    fun getWordsPerMinute(): Double = if (audioRecordingDuration > 0) {
        (wordCount.toDouble() / (audioRecordingDuration / 60000.0))
    } else 0.0
    
    fun getCharactersPerSecond(): Double = if (audioRecordingDuration > 0) {
        (characterCount.toDouble() / (audioRecordingDuration / 1000.0))
    } else 0.0
    
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate word count
        if (wordCount < 0) {
            errors.add("Word count cannot be negative")
        } else if (wordCount > MAX_WORD_COUNT) {
            errors.add("Word count exceeds maximum limit of $MAX_WORD_COUNT")
        }
        
        // Validate character count
        if (characterCount < 0) {
            errors.add("Character count cannot be negative")
        } else if (characterCount > MAX_CHARACTER_COUNT) {
            errors.add("Character count exceeds maximum limit of $MAX_CHARACTER_COUNT")
        }
        
        // Validate speaking rate
        if (speakingRate < 0) {
            errors.add("Speaking rate cannot be negative")
        } else if (speakingRate > MAX_SPEAKING_RATE) {
            errors.add("Speaking rate exceeds maximum limit of $MAX_SPEAKING_RATE WPM")
        }
        
        // Validate audio duration
        if (audioRecordingDuration < 0) {
            errors.add("Audio recording duration cannot be negative")
        } else if (audioRecordingDuration > MAX_AUDIO_DURATION_MS) {
            errors.add("Audio recording duration exceeds maximum limit of ${MAX_AUDIO_DURATION_MS / 60000} minutes")
        }
        
        // Validate file size
        if (audioFileSize < 0) {
            errors.add("Audio file size cannot be negative")
        } else if (audioFileSize > MAX_AUDIO_FILE_SIZE) {
            errors.add("Audio file size exceeds maximum limit of ${MAX_AUDIO_FILE_SIZE / 1_000_000}MB")
        }
        
        // Validate transcription text length
        transcriptionText?.let { text ->
            if (text.length > MAX_TRANSCRIPTION_LENGTH) {
                errors.add("Transcription text length exceeds maximum limit of $MAX_TRANSCRIPTION_LENGTH characters")
            }
        }
        
        // Validate session timing
        sessionEndTime?.let { endTime ->
            if (endTime < sessionStartTime) {
                errors.add("Session end time cannot be before start time")
            }
        }
        
        // Validate word/character consistency
        if (wordCount > 0 && characterCount == 0) {
            errors.add("Character count should be greater than 0 when word count is positive")
        }
        
        return if (errors.isEmpty()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(errors)
        }
    }
    
    fun sanitized(): SessionMetrics {
        return copy(
            wordCount = wordCount.coerceIn(0, MAX_WORD_COUNT),
            characterCount = characterCount.coerceIn(0, MAX_CHARACTER_COUNT),
            speakingRate = speakingRate.coerceIn(MIN_SPEAKING_RATE, MAX_SPEAKING_RATE),
            audioRecordingDuration = audioRecordingDuration.coerceIn(0, MAX_AUDIO_DURATION_MS),
            audioFileSize = audioFileSize.coerceIn(0, MAX_AUDIO_FILE_SIZE),
            transcriptionText = transcriptionText?.take(MAX_TRANSCRIPTION_LENGTH)
        )
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val errors: List<String>) : ValidationResult()
}

@Serializable
data class MetricsAggregation(
    val periodStart: Long,
    val periodEnd: Long,
    val totalSessions: Int = 0,
    val successfulSessions: Int = 0,
    val averageRecordingDuration: Long = 0,
    val averageTranscriptionTime: Long = 0,
    val averageMemoryUsage: Long = 0,
    val peakMemoryUsage: Long = 0,
    val totalApiCalls: Int = 0,
    val failedApiCalls: Int = 0,
    val totalRetries: Int = 0,
    val commonErrors: Map<String, Int> = emptyMap(),
    val performanceWarnings: List<PerformanceWarning> = emptyList()
) {
    fun getSuccessRate(): Float = if (totalSessions > 0) (successfulSessions.toFloat() / totalSessions) * 100 else 0f
    
    fun getApiErrorRate(): Float = if (totalApiCalls > 0) (failedApiCalls.toFloat() / totalApiCalls) * 100 else 0f
    
    fun getAverageRetryRate(): Float = if (totalApiCalls > 0) totalRetries.toFloat() / totalApiCalls else 0f
}