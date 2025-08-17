package me.shadykhalifa.whispertop.data.services

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.domain.models.*
import me.shadykhalifa.whispertop.domain.services.LoggingManager
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.domain.services.MetricsExportFormat
class MetricsCollectorImpl(
    private val loggingManager: LoggingManager
) : MetricsCollector {
    
    private val sessions = mutableMapOf<String, PerformanceSession>()
    private val mutex = Mutex()
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    override suspend fun startSession(sessionId: String): PerformanceSession = mutex.withLock {
        val session = PerformanceSession(
            sessionId = sessionId,
            startTime = System.currentTimeMillis()
        )
        sessions[sessionId] = session
        
        loggingManager.debug(
            message = "Started performance session: $sessionId",
            component = "MetricsCollector"
        )
        
        session
    }
    
    override suspend fun endSession(sessionId: String): PerformanceSession? = mutex.withLock {
        val session = sessions[sessionId]?.copy(endTime = System.currentTimeMillis())
        if (session != null) {
            sessions[sessionId] = session
            
            loggingManager.info(
                message = "Ended performance session: $sessionId, duration: ${session.getTotalSessionTime()}, success: ${session.getOverallSuccess()}",
                component = "MetricsCollector"
            )
        }
        session
    }
    
    override suspend fun startRecordingMetrics(sessionId: String): RecordingMetrics = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        val deviceInfo = getDeviceInfo()
        
        val recordingMetrics = RecordingMetrics(
            sessionId = sessionId,
            startTime = currentTime,
            deviceInfo = deviceInfo
        )
        
        updateSessionRecordingMetrics(sessionId, recordingMetrics)
        
        loggingManager.debug(
            message = "Started recording metrics: $sessionId",
            component = "MetricsCollector"
        )
        
        recordingMetrics
    }
    
    override suspend fun updateRecordingMetrics(
        sessionId: String, 
        updateFunc: (RecordingMetrics) -> RecordingMetrics
    ) = mutex.withLock {
        val session = sessions[sessionId]
        val currentMetrics = session?.recordingMetrics
        if (currentMetrics != null) {
            val updatedMetrics = updateFunc(currentMetrics)
            updateSessionRecordingMetrics(sessionId, updatedMetrics)
        }
    }
    
    override suspend fun endRecordingMetrics(
        sessionId: String, 
        success: Boolean, 
        error: String?
    ) = mutex.withLock {
        val session = sessions[sessionId]
        val currentMetrics = session?.recordingMetrics
        if (currentMetrics != null) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - currentMetrics.startTime
            
            val finalMetrics = currentMetrics.copy(
                endTime = endTime,
                duration = duration,
                success = success,
                errorMessage = error,
                errorType = if (error != null) classifyError(error) else null
            )
            
            updateSessionRecordingMetrics(sessionId, finalMetrics)
            
            // Check for performance warnings
            checkRecordingWarnings(sessionId, finalMetrics)
            
            loggingManager.info(
                message = "Ended recording metrics: $sessionId, duration: $duration, success: $success, audioFileSize: ${finalMetrics.audioFileSize}, peakMemoryUsage: ${finalMetrics.peakMemoryUsage}",
                component = "MetricsCollector"
            )
        }
    }
    
    override suspend fun startTranscriptionMetrics(sessionId: String): TranscriptionMetrics = mutex.withLock {
        val currentTime = System.currentTimeMillis()
        
        val transcriptionMetrics = TranscriptionMetrics(
            sessionId = sessionId,
            requestStartTime = currentTime
        )
        
        updateSessionTranscriptionMetrics(sessionId, transcriptionMetrics)
        
        loggingManager.debug(
            message = "Started transcription metrics: $sessionId",
            component = "MetricsCollector"
        )
        
        transcriptionMetrics
    }
    
    override suspend fun updateTranscriptionMetrics(
        sessionId: String, 
        updateFunc: (TranscriptionMetrics) -> TranscriptionMetrics
    ) = mutex.withLock {
        val session = sessions[sessionId]
        val currentMetrics = session?.transcriptionMetrics
        if (currentMetrics != null) {
            val updatedMetrics = updateFunc(currentMetrics)
            updateSessionTranscriptionMetrics(sessionId, updatedMetrics)
        }
    }
    
    override suspend fun endTranscriptionMetrics(
        sessionId: String, 
        success: Boolean, 
        error: String?
    ) = mutex.withLock {
        val session = sessions[sessionId]
        val currentMetrics = session?.transcriptionMetrics
        if (currentMetrics != null) {
            val endTime = System.currentTimeMillis()
            val duration = endTime - currentMetrics.requestStartTime
            
            val finalMetrics = currentMetrics.copy(
                requestEndTime = endTime,
                apiCallDuration = duration,
                success = success,
                errorMessage = error,
                errorType = if (error != null) classifyError(error) else null
            )
            
            updateSessionTranscriptionMetrics(sessionId, finalMetrics)
            
            // Check for performance warnings
            checkTranscriptionWarnings(sessionId, finalMetrics)
            
            loggingManager.info(
                message = "Ended transcription metrics: $sessionId, duration: $duration, success: $success, transcriptionLength: ${finalMetrics.transcriptionLength}, model: ${finalMetrics.model}, retryCount: ${finalMetrics.retryCount}",
                component = "MetricsCollector"
            )
        }
    }
    
    override suspend fun recordMemorySnapshot(sessionId: String, context: String) = mutex.withLock {
        val session = sessions[sessionId]
        if (session != null) {
            val memory = getCurrentMemoryInfo()
            val snapshot = MemorySnapshot(
                timestamp = System.currentTimeMillis(),
                usedMemory = memory.usedMemory,
                freeMemory = memory.freeMemory,
                totalMemory = memory.totalMemory,
                heapSize = memory.usedMemory,
                context = context
            )
            
            val updatedSnapshots = session.memorySnapshots + snapshot
            sessions[sessionId] = session.copy(memorySnapshots = updatedSnapshots)
            
            // Check for memory warnings
            if (snapshot.getUsagePercentage() > 80f) {
                val warning = PerformanceWarning(
                    timestamp = snapshot.timestamp,
                    type = WarningType.HIGH_MEMORY_USAGE,
                    message = "High memory usage detected: ${snapshot.getUsagePercentage().toInt()}%",
                    severity = if (snapshot.getUsagePercentage() > 90f) WarningSeverity.CRITICAL else WarningSeverity.WARNING,
                    context = context,
                    metrics = mapOf(
                        "usagePercentage" to snapshot.getUsagePercentage().toString(),
                        "usedMemory" to snapshot.usedMemory.toString(),
                        "totalMemory" to snapshot.totalMemory.toString()
                    )
                )
                recordPerformanceWarning(sessionId, warning)
            }
        }
    }
    
    override suspend fun recordPerformanceWarning(
        sessionId: String, 
        warning: PerformanceWarning
    ) = mutex.withLock {
        val session = sessions[sessionId]
        if (session != null) {
            val updatedWarnings = session.performanceWarnings + warning
            sessions[sessionId] = session.copy(performanceWarnings = updatedWarnings)
            
            loggingManager.warn(
                message = "Performance warning: ${warning.message}, sessionId: $sessionId, type: ${warning.type.name}, severity: ${warning.severity.name}, context: ${warning.context}",
                component = "MetricsCollector"
            )
        }
    }
    
    override suspend fun getSession(sessionId: String): PerformanceSession? = mutex.withLock {
        sessions[sessionId]
    }
    
    override suspend fun getAllSessions(): List<PerformanceSession> = mutex.withLock {
        sessions.values.toList()
    }
    
    override suspend fun getMetricsAggregation(startTime: Long, endTime: Long): MetricsAggregation = mutex.withLock {
        val sessionsInPeriod = sessions.values.filter { 
            it.startTime >= startTime && it.startTime <= endTime 
        }
        
        val successfulSessions = sessionsInPeriod.count { it.getOverallSuccess() }
        val totalRecordingDuration = sessionsInPeriod.mapNotNull { it.recordingMetrics?.duration }.sum()
        val totalTranscriptionTime = sessionsInPeriod.mapNotNull { it.transcriptionMetrics?.apiCallDuration }.sum()
        val memoryUsages = sessionsInPeriod.flatMap { it.memorySnapshots.map { snapshot -> snapshot.usedMemory } }
        val totalApiCalls = sessionsInPeriod.mapNotNull { it.transcriptionMetrics }.size
        val failedApiCalls = sessionsInPeriod.count { it.transcriptionMetrics?.success == false }
        val totalRetries = sessionsInPeriod.mapNotNull { it.transcriptionMetrics?.retryCount }.sum()
        
        val errorCounts = mutableMapOf<String, Int>()
        sessionsInPeriod.forEach { session ->
            session.recordingMetrics?.errorType?.let { errorType ->
                errorCounts[errorType] = errorCounts.getOrDefault(errorType, 0) + 1
            }
            session.transcriptionMetrics?.errorType?.let { errorType ->
                errorCounts[errorType] = errorCounts.getOrDefault(errorType, 0) + 1
            }
        }
        
        val allWarnings = sessionsInPeriod.flatMap { it.performanceWarnings }
        
        MetricsAggregation(
            periodStart = startTime,
            periodEnd = endTime,
            totalSessions = sessionsInPeriod.size,
            successfulSessions = successfulSessions,
            averageRecordingDuration = if (sessionsInPeriod.isNotEmpty()) totalRecordingDuration / sessionsInPeriod.size else 0,
            averageTranscriptionTime = if (totalApiCalls > 0) totalTranscriptionTime / totalApiCalls else 0,
            averageMemoryUsage = if (memoryUsages.isNotEmpty()) memoryUsages.sum() / memoryUsages.size else 0,
            peakMemoryUsage = memoryUsages.maxOrNull() ?: 0,
            totalApiCalls = totalApiCalls,
            failedApiCalls = failedApiCalls,
            totalRetries = totalRetries,
            commonErrors = errorCounts,
            performanceWarnings = allWarnings
        )
    }
    
    override suspend fun clearOldSessions(olderThan: Long) = mutex.withLock {
        val before = sessions.size
        sessions.entries.removeAll { it.value.startTime < olderThan }
        val after = sessions.size
        
        loggingManager.info(
            message = "Cleared old performance sessions: removed ${before - after}, remaining $after",
            component = "MetricsCollector"
        )
    }
    
    override suspend fun exportMetrics(format: MetricsExportFormat): String = mutex.withLock {
        return when (format) {
            MetricsExportFormat.JSON -> {
                json.encodeToString(sessions.values.toList())
            }
            MetricsExportFormat.CSV -> {
                exportToCsv()
            }
        }
    }
    
    private fun updateSessionRecordingMetrics(sessionId: String, metrics: RecordingMetrics) {
        val session = sessions[sessionId]
        if (session != null) {
            sessions[sessionId] = session.copy(recordingMetrics = metrics)
        }
    }
    
    private fun updateSessionTranscriptionMetrics(sessionId: String, metrics: TranscriptionMetrics) {
        val session = sessions[sessionId]
        if (session != null) {
            sessions[sessionId] = session.copy(transcriptionMetrics = metrics)
        }
    }
    
    private suspend fun checkRecordingWarnings(sessionId: String, metrics: RecordingMetrics) {
        // Check for long recording sessions
        if (metrics.duration > 300_000) { // 5 minutes
            recordPerformanceWarning(sessionId, PerformanceWarning(
                timestamp = System.currentTimeMillis(),
                type = WarningType.LONG_RECORDING_SESSION,
                message = "Recording session exceeded 5 minutes: ${metrics.duration / 1000}s",
                severity = WarningSeverity.WARNING,
                context = "recording",
                metrics = mapOf("duration" to metrics.duration.toString())
            ))
        }
        
        // Check for buffer underruns
        if (metrics.bufferUnderrunCount > 0) {
            recordPerformanceWarning(sessionId, PerformanceWarning(
                timestamp = System.currentTimeMillis(),
                type = WarningType.BUFFER_UNDERRUN,
                message = "Audio buffer underruns detected: ${metrics.bufferUnderrunCount}",
                severity = if (metrics.bufferUnderrunCount > 5) WarningSeverity.CRITICAL else WarningSeverity.WARNING,
                context = "recording",
                metrics = mapOf("underrunCount" to metrics.bufferUnderrunCount.toString())
            ))
        }
    }
    
    private suspend fun checkTranscriptionWarnings(sessionId: String, metrics: TranscriptionMetrics) {
        // Check for slow transcription
        if (metrics.apiCallDuration > 10_000) { // 10 seconds
            recordPerformanceWarning(sessionId, PerformanceWarning(
                timestamp = System.currentTimeMillis(),
                type = WarningType.SLOW_TRANSCRIPTION,
                message = "Transcription took longer than expected: ${metrics.apiCallDuration}ms",
                severity = WarningSeverity.WARNING,
                context = "transcription",
                metrics = mapOf("duration" to metrics.apiCallDuration.toString(), "model" to metrics.model)
            ))
        }
        
        // Check for high retry count
        if (metrics.retryCount > 2) {
            recordPerformanceWarning(sessionId, PerformanceWarning(
                timestamp = System.currentTimeMillis(),
                type = WarningType.API_ERROR_RATE,
                message = "High API retry count: ${metrics.retryCount}",
                severity = WarningSeverity.WARNING,
                context = "transcription",
                metrics = mapOf("retryCount" to metrics.retryCount.toString())
            ))
        }
        
        // Check for network latency
        if (metrics.connectionTimeMs > 5000) { // 5 seconds
            recordPerformanceWarning(sessionId, PerformanceWarning(
                timestamp = System.currentTimeMillis(),
                type = WarningType.NETWORK_LATENCY,
                message = "High network latency detected: ${metrics.connectionTimeMs}ms",
                severity = WarningSeverity.WARNING,
                context = "network",
                metrics = mapOf("latency" to metrics.connectionTimeMs.toString())
            ))
        }
    }
    
    private fun classifyError(error: String): String {
        return when {
            error.contains("network", ignoreCase = true) -> "NetworkError"
            error.contains("timeout", ignoreCase = true) -> "TimeoutError"
            error.contains("authentication", ignoreCase = true) -> "AuthenticationError"
            error.contains("rate limit", ignoreCase = true) -> "RateLimitError"
            error.contains("memory", ignoreCase = true) -> "MemoryError"
            error.contains("storage", ignoreCase = true) -> "StorageError"
            else -> "UnknownError"
        }
    }
    
    private fun getCurrentMemoryInfo(): MemoryUsageInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        
        return MemoryUsageInfo(
            usedMemory = usedMemory,
            freeMemory = freeMemory,
            totalMemory = runtime.maxMemory()
        )
    }
    
    private fun getDeviceInfo(): DeviceInfo {
        val memory = getCurrentMemoryInfo()
        return DeviceInfo(
            manufacturer = getDeviceManufacturer(),
            model = getDeviceModel(),
            osVersion = getOSVersion(),
            appVersion = getAppVersion(),
            availableMemory = memory.freeMemory,
            totalMemory = memory.totalMemory,
            cpuCoreCount = getCpuCoreCount(),
            architecture = getArchitecture()
        )
    }
    
    private fun exportToCsv(): String {
        val csv = StringBuilder()
        csv.appendLine("SessionId,StartTime,EndTime,Duration,RecordingSuccess,TranscriptionSuccess,RecordingDuration,TranscriptionTime,AudioFileSize,PeakMemory,Errors")
        
        sessions.values.forEach { session ->
            csv.appendLine(
                "${session.sessionId}," +
                "${session.startTime}," +
                "${session.endTime ?: ""}," +
                "${session.getTotalSessionTime()}," +
                "${session.recordingMetrics?.success ?: false}," +
                "${session.transcriptionMetrics?.success ?: false}," +
                "${session.recordingMetrics?.duration ?: 0}," +
                "${session.transcriptionMetrics?.apiCallDuration ?: 0}," +
                "${session.recordingMetrics?.audioFileSize ?: 0}," +
                "${session.recordingMetrics?.peakMemoryUsage ?: 0}," +
                "\"${session.performanceWarnings.size}\""
            )
        }
        
        return csv.toString()
    }
}

private data class MemoryUsageInfo(
    val usedMemory: Long,
    val freeMemory: Long,
    val totalMemory: Long
)

// Platform-specific expect declarations
expect fun getDeviceManufacturer(): String
expect fun getDeviceModel(): String
expect fun getOSVersion(): String
expect fun getAppVersion(): String
expect fun getCpuCoreCount(): Int
expect fun getArchitecture(): String