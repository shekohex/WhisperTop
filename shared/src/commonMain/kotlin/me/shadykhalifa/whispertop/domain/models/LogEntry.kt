package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

/**
 * Represents a comprehensive log entry with rich metadata for debugging and monitoring.
 */
@Serializable
data class LogEntry(
    val timestamp: Long = getCurrentTimeMillis(),
    val level: LogLevel,
    val component: String,
    val message: String,
    val threadInfo: ThreadInfo? = null,
    val sessionId: String,
    val metadata: Map<String, String> = emptyMap(),
    val exception: ExceptionInfo? = null,
    val performanceMetrics: PerformanceMetrics? = null
)

/**
 * Information about the thread where the log was generated
 */
@Serializable
data class ThreadInfo(
    val threadName: String,
    val threadId: Long
) {
    companion object {
        fun current(): ThreadInfo {
            return ThreadInfo(
                threadName = getCurrentThreadName(),
                threadId = getCurrentThreadId()
            )
        }
    }
}

/**
 * Structured exception information for logging
 */
@Serializable
data class ExceptionInfo(
    val type: String,
    val message: String,
    val stackTrace: String,
    val cause: String? = null
) {
    companion object {
        fun from(throwable: Throwable): ExceptionInfo {
            return ExceptionInfo(
                type = throwable::class.simpleName ?: "UnknownException",
                message = throwable.message ?: "No message",
                stackTrace = throwable.stackTraceToString(),
                cause = throwable.cause?.let { "${it::class.simpleName}: ${it.message}" }
            )
        }
    }
}

/**
 * Performance metrics for operation tracking
 */
@Serializable
data class PerformanceMetrics(
    val operationName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long = endTime - startTime,
    val memoryUsageMb: Double? = null,
    val additionalMetrics: Map<String, String> = emptyMap()
)

/**
 * Structured log context for specific operations or components
 */
@Serializable
data class LogContext(
    val operationId: String? = null,
    val userId: String? = null,
    val requestId: String? = null,
    val component: String,
    val additionalContext: Map<String, String> = emptyMap()
) {
    fun toMetadata(): Map<String, String> {
        val metadata = mutableMapOf<String, String>()
        operationId?.let { metadata["operation_id"] = it }
        userId?.let { metadata["user_id"] = it }
        requestId?.let { metadata["request_id"] = it }
        metadata["component"] = component
        metadata.putAll(additionalContext)
        return metadata
    }
}

// Platform-specific implementations for thread info
expect fun getCurrentThreadName(): String
expect fun getCurrentThreadId(): Long
expect fun getCurrentTimeMillis(): Long