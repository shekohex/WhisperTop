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