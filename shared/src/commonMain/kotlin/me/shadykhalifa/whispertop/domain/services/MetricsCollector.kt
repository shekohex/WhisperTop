package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.*

interface MetricsCollector {
    suspend fun startSession(sessionId: String): PerformanceSession
    suspend fun endSession(sessionId: String): PerformanceSession?
    suspend fun startRecordingMetrics(sessionId: String): RecordingMetrics
    suspend fun updateRecordingMetrics(sessionId: String, updateFunc: (RecordingMetrics) -> RecordingMetrics)
    suspend fun endRecordingMetrics(sessionId: String, success: Boolean, error: String? = null)
    suspend fun startTranscriptionMetrics(sessionId: String): TranscriptionMetrics
    suspend fun updateTranscriptionMetrics(sessionId: String, updateFunc: (TranscriptionMetrics) -> TranscriptionMetrics)
    suspend fun endTranscriptionMetrics(sessionId: String, success: Boolean, error: String? = null)
    suspend fun recordMemorySnapshot(sessionId: String, context: String = "")
    suspend fun recordPerformanceWarning(sessionId: String, warning: PerformanceWarning)
    suspend fun getSession(sessionId: String): PerformanceSession?
    suspend fun getAllSessions(): List<PerformanceSession>
    suspend fun getMetricsAggregation(startTime: Long, endTime: Long): MetricsAggregation
    suspend fun clearOldSessions(olderThan: Long)
    suspend fun exportMetrics(format: MetricsExportFormat): String
}

enum class MetricsExportFormat {
    JSON,
    CSV
}