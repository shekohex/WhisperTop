package me.shadykhalifa.whispertop.data.services

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.util.*
import kotlinx.serialization.json.*
import me.shadykhalifa.whispertop.domain.services.DebugLoggingService
import me.shadykhalifa.whispertop.domain.services.LoggingManager

class DebugLoggingServiceImpl(
    private val loggingManager: LoggingManager
) : DebugLoggingService {
    
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    companion object {
        private const val COMPONENT = "DebugLogging"
        private const val MAX_BODY_SIZE = 10 * 1024 // 10KB max body size for logging
        private const val MAX_HEADER_VALUE_LENGTH = 200
    }
    
    override fun isDebugBuild(): Boolean {
        return getDebugBuildFlag()
    }
    
    override suspend fun logRequest(
        request: HttpRequestBuilder,
        requestId: String,
        timestamp: Long
    ) {
        if (!isDebugBuild()) return
        
        try {
            val sanitizedHeaders = sanitizeHeaders(request.headers.build())
            val bodyInfo = extractBodyInfo(request.body as? OutgoingContent)
            
            val requestInfo = buildJsonObject {
                put("requestId", requestId)
                put("timestamp", timestamp)
                put("method", request.method.value)
                put("url", sanitizeUrl(request.url.buildString()))
                put("headers", sanitizedHeaders)
                put("bodyType", bodyInfo.type)
                put("bodySize", bodyInfo.size)
                if (bodyInfo.preview.isNotEmpty()) {
                    put("bodyPreview", bodyInfo.preview)
                }
            }
            
            loggingManager.debug(
                message = "HTTP Request Details: $requestId - ${request.method.value} ${sanitizeUrl(request.url.buildString())} (${bodyInfo.size} bytes)",
                component = COMPONENT
            )
            
            // Log the detailed JSON in verbose mode
            loggingManager.verbose(
                message = "Request JSON: $requestInfo",
                component = COMPONENT
            )
            
        } catch (e: Exception) {
            loggingManager.error(
                message = "Failed to log request details: ${e.message}",
                component = COMPONENT,
                exception = e
            )
        }
    }
    
    override suspend fun logResponse(
        response: HttpResponse,
        requestId: String,
        timestamp: Long,
        duration: Long
    ) {
        if (!isDebugBuild()) return
        
        try {
            val sanitizedHeaders = sanitizeHeaders(response.headers)
            val bodyText = try {
                if (response.contentLength() ?: 0 <= MAX_BODY_SIZE) {
                    val content = response.bodyAsText()
                    sanitizeResponseBody(content)
                } else {
                    "[Body too large: ${response.contentLength()} bytes]"
                }
            } catch (e: Exception) {
                "[Error reading body: ${e.message}]"
            }
            
            val responseInfo = buildJsonObject {
                put("requestId", requestId)
                put("timestamp", timestamp)
                put("duration", duration)
                put("status", response.status.value)
                put("statusDescription", response.status.description)
                put("headers", sanitizedHeaders)
                put("contentType", response.contentType()?.toString() ?: "unknown")
                put("contentLength", response.contentLength() ?: -1)
                put("bodyPreview", bodyText.take(500))
                put("httpVersion", response.version.toString())
            }
            
            loggingManager.debug(
                message = "HTTP Response Details: $requestId - ${response.status.value} ${response.status.description} (${duration}ms, ${response.contentLength() ?: -1} bytes)",
                component = COMPONENT
            )
            
            // Log detailed JSON in verbose mode
            loggingManager.verbose(
                message = "Response JSON: $responseInfo",
                component = COMPONENT
            )
            
            // Log performance warnings for slow responses
            if (duration > 10000) { // 10 seconds
                loggingManager.warn(
                    message = "Slow API response detected: ${duration}ms for request $requestId",
                    component = COMPONENT
                )
            }
            
        } catch (e: Exception) {
            loggingManager.error(
                message = "Failed to log response details: ${e.message}",
                component = COMPONENT,
                exception = e
            )
        }
    }
    
    override suspend fun logNetworkError(
        error: Throwable,
        requestId: String,
        timestamp: Long,
        context: String
    ) {
        if (!isDebugBuild()) return
        
        val errorInfo = buildJsonObject {
            put("requestId", requestId)
            put("timestamp", timestamp)
            put("context", context)
            put("errorType", error::class.simpleName ?: "Unknown")
            put("errorMessage", error.message ?: "No message")
            put("stackTrace", error.stackTraceToString().take(1000))
        }
        
        loggingManager.error(
            message = "Network Error in $context: ${error.message}",
            component = COMPONENT,
            exception = error
        )
        
        loggingManager.verbose(
            message = "Network Error JSON: $errorInfo",
            component = COMPONENT
        )
    }
    
    override suspend fun logRetryAttempt(
        attempt: Int,
        maxAttempts: Int,
        requestId: String,
        reason: String,
        delayMs: Long
    ) {
        if (!isDebugBuild()) return
        
        loggingManager.warn(
            message = "Retry attempt $attempt/$maxAttempts for request $requestId: $reason (delay: ${delayMs}ms)",
            component = COMPONENT
        )
    }
    
    override suspend fun logConnectionMetrics(
        activeConnections: Int,
        idleConnections: Int,
        queuedRequests: Int
    ) {
        if (!isDebugBuild()) return
        
        loggingManager.debug(
            message = "Connection Pool Metrics: $activeConnections active, $idleConnections idle, $queuedRequests queued",
            component = COMPONENT
        )
        
        // Warn if connection pool is getting saturated
        val totalConnections = activeConnections + idleConnections
        if (queuedRequests > 0 || activeConnections > totalConnections * 0.8) {
            loggingManager.warn(
                message = "High connection pool usage: $activeConnections active, $queuedRequests queued",
                component = COMPONENT
            )
        }
    }
    
    override suspend fun logRequestTiming(
        requestId: String,
        dnsLookupTime: Long,
        connectionTime: Long,
        sslHandshakeTime: Long,
        requestTime: Long,
        responseTime: Long,
        totalTime: Long
    ) {
        if (!isDebugBuild()) return
        
        val timingInfo = buildJsonObject {
            put("requestId", requestId)
            put("dnsLookupTime", dnsLookupTime)
            put("connectionTime", connectionTime)
            put("sslHandshakeTime", sslHandshakeTime)
            put("requestTime", requestTime)
            put("responseTime", responseTime)
            put("totalTime", totalTime)
            put("networkOverhead", totalTime - requestTime - responseTime)
        }
        
        loggingManager.debug(
            message = "Request Timing Breakdown: $requestId - DNS: ${dnsLookupTime}ms, Connect: ${connectionTime}ms, SSL: ${sslHandshakeTime}ms, Total: ${totalTime}ms",
            component = COMPONENT
        )
        
        loggingManager.verbose(
            message = "Timing JSON: $timingInfo",
            component = COMPONENT
        )
        
        // Log warnings for performance issues
        when {
            dnsLookupTime > 5000 -> loggingManager.warn(
                message = "Slow DNS lookup: ${dnsLookupTime}ms for request $requestId",
                component = COMPONENT
            )
            connectionTime > 10000 -> loggingManager.warn(
                message = "Slow connection establishment: ${connectionTime}ms for request $requestId",
                component = COMPONENT
            )
            sslHandshakeTime > 5000 -> loggingManager.warn(
                message = "Slow SSL handshake: ${sslHandshakeTime}ms for request $requestId",
                component = COMPONENT
            )
        }
    }
    
    private fun sanitizeHeaders(headers: Headers): JsonObject {
        return buildJsonObject {
            headers.forEach { name, values ->
                val sanitizedName = name.lowercase()
                when {
                    sanitizedName.contains("authorization") ||
                    sanitizedName.contains("api-key") ||
                    sanitizedName.contains("token") -> {
                        put(name, "[REDACTED]")
                    }
                    sanitizedName.contains("user-agent") ||
                    sanitizedName.contains("content-type") ||
                    sanitizedName.contains("accept") ||
                    sanitizedName.contains("content-length") -> {
                        put(name, values.firstOrNull()?.take(MAX_HEADER_VALUE_LENGTH) ?: "")
                    }
                    else -> {
                        put(name, values.joinToString(", ").take(MAX_HEADER_VALUE_LENGTH))
                    }
                }
            }
        }
    }
    
    private fun sanitizeUrl(url: String): String {
        // Remove any potential API keys from URL parameters
        return url.replace(Regex("[?&]api_key=[^&]*"), "?api_key=[REDACTED]")
            .replace(Regex("[?&]key=[^&]*"), "?key=[REDACTED]")
            .replace(Regex("[?&]token=[^&]*"), "?token=[REDACTED]")
    }
    
    private fun sanitizeResponseBody(body: String): String {
        // Remove any potential sensitive data from response body
        var sanitized = body
        
        // Remove any API keys or tokens that might be in the response
        sanitized = sanitized.replace(Regex("\"api_key\"\\s*:\\s*\"[^\"]+\""), "\"api_key\": \"[REDACTED]\"")
        sanitized = sanitized.replace(Regex("\"token\"\\s*:\\s*\"[^\"]+\""), "\"token\": \"[REDACTED]\"")
        sanitized = sanitized.replace(Regex("\"key\"\\s*:\\s*\"[^\"]+\""), "\"key\": \"[REDACTED]\"")
        
        // Try to format JSON for better readability
        return try {
            val jsonElement = json.parseToJsonElement(sanitized)
            jsonElement.toString()
        } catch (e: Exception) {
            sanitized
        }
    }
    
    private data class BodyInfo(
        val type: String,
        val size: Long,
        val preview: String
    )
    
    private fun extractBodyInfo(body: OutgoingContent?): BodyInfo {
        return when (body) {
            null -> BodyInfo("none", 0, "")
            is OutgoingContent.ByteArrayContent -> {
                val bytes = body.bytes()
                BodyInfo(
                    type = "byte_array",
                    size = bytes.size.toLong(),
                    preview = if (bytes.size <= 100) "[${bytes.size} bytes]" else "[${bytes.size} bytes - too large for preview]"
                )
            }
            is OutgoingContent.ReadChannelContent -> BodyInfo(
                type = "read_channel",
                size = body.contentLength ?: -1,
                preview = "[ReadChannel content - size: ${body.contentLength ?: "unknown"}]"
            )
            is OutgoingContent.WriteChannelContent -> BodyInfo(
                type = "write_channel",
                size = body.contentLength ?: -1,
                preview = "[WriteChannel content - size: ${body.contentLength ?: "unknown"}]"
            )
            is OutgoingContent.NoContent -> BodyInfo("no_content", 0, "")
            else -> BodyInfo(
                type = body::class.simpleName ?: "unknown",
                size = -1,
                preview = "[${body::class.simpleName} content]"
            )
        }
    }
}

// Platform-specific expect/actual for debug build detection
expect fun getDebugBuildFlag(): Boolean