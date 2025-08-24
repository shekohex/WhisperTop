package me.shadykhalifa.whispertop.domain.models

import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.utils.TimeUtils

data class ErrorContext(
    val operationName: String,
    val userId: String? = null,
    val sessionId: String? = null,
    val timestamp: Long = TimeUtils.currentTimeMillis(),
    val additionalData: Map<String, Any> = emptyMap()
)

interface ErrorMapper {
    fun mapToErrorInfo(error: Throwable, context: String? = null): ErrorInfo
    fun mapToErrorInfoWithContext(error: Throwable, errorContext: ErrorContext): ErrorInfo
}

class ErrorMapperImpl(
    private val errorLoggingService: ErrorLoggingService
) : ErrorMapper {
    
    override fun mapToErrorInfo(error: Throwable, context: String?): ErrorInfo {
        val errorContext = ErrorContext(
            operationName = context ?: "unknown_operation"
        )
        return mapToErrorInfoWithContext(error, errorContext)
    }
    
    override fun mapToErrorInfoWithContext(error: Throwable, errorContext: ErrorContext): ErrorInfo {
        logErrorWithContext(error, errorContext)
        return ErrorClassifier.classifyError(error)
    }
    
    private fun logErrorWithContext(error: Throwable, context: ErrorContext) {
        val enrichedContext = buildMap {
            put("operation", context.operationName)
            put("timestamp", context.timestamp.toString())
            context.userId?.let { put("user_id", it) }
            context.sessionId?.let { put("session_id", it) }
            context.additionalData.forEach { (key, value) ->
                put(key, value.toString())
            }
        }
        
        errorLoggingService.logError(
            error = error,
            context = enrichedContext,
            additionalInfo = "Mapped error from ${context.operationName}"
        )
    }
}