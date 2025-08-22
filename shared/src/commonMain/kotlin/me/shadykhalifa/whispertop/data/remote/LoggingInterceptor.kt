package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.util.*
import me.shadykhalifa.whispertop.domain.services.DebugLoggingService
import me.shadykhalifa.whispertop.utils.TimeUtils
import me.shadykhalifa.whispertop.domain.services.LoggingManager

enum class OpenAILogLevel {
    NONE,
    BASIC,
    HEADERS,
    BODY,
    DEBUG  // New debug level for comprehensive logging
}

class OpenAILoggingInterceptor(
    private val logLevel: OpenAILogLevel = OpenAILogLevel.BASIC,
    private val debugLoggingService: DebugLoggingService? = null,
    private val loggingManager: LoggingManager? = null
) {
    
    private val requestTimings = mutableMapOf<String, RequestTiming>()
    
    private data class RequestTiming(
        val requestId: String,
        val startTime: Long,
        var dnsLookupTime: Long = 0,
        var connectionTime: Long = 0,
        var sslHandshakeTime: Long = 0,
        var requestSentTime: Long = 0,
        var responseReceivedTime: Long = 0
    )
    
    fun getKtorLogLevel(): LogLevel {
        return when (logLevel) {
            OpenAILogLevel.NONE -> LogLevel.NONE
            OpenAILogLevel.BASIC -> LogLevel.INFO
            OpenAILogLevel.HEADERS -> LogLevel.HEADERS
            OpenAILogLevel.BODY -> LogLevel.ALL
            OpenAILogLevel.DEBUG -> LogLevel.ALL
        }
    }
    
    fun createLogger(): Logger {
        return object : Logger {
            override fun log(message: String) {
                when (logLevel) {
                    OpenAILogLevel.NONE -> {}
                    OpenAILogLevel.DEBUG -> {
                        // Enhanced logging for debug mode
                        val sanitizedMessage = sanitizeLogMessage(message)
                        loggingManager?.debug(
                            message = sanitizedMessage,
                            component = "OpenAI-API"
                        ) ?: println("[OpenAI-API-DEBUG] $sanitizedMessage")
                    }
                    else -> {
                        val sanitizedMessage = sanitizeLogMessage(message)
                        loggingManager?.info(
                            message = sanitizedMessage,
                            component = "OpenAI-API"
                        ) ?: println("[OpenAI-API] $sanitizedMessage")
                    }
                }
            }
        }
    }
    
    private fun sanitizeLogMessage(message: String): String {
        return message
            .replace(Regex("Bearer [A-Za-z0-9-._~+/]+=*"), "Bearer [REDACTED]")
            .replace(Regex("\"api_key\"\\s*:\\s*\"[^\"]+\""), "\"api_key\": \"[REDACTED]\"")
            .replace(Regex("Authorization: Bearer [A-Za-z0-9-._~+/]+=*"), "Authorization: Bearer [REDACTED]")
            .replace(Regex("sk-[A-Za-z0-9]{32,}"), "sk-[REDACTED]")
            .replace(Regex("\"token\"\\s*:\\s*\"[^\"]+\""), "\"token\": \"[REDACTED]\"")
            .replace(Regex("\"key\"\\s*:\\s*\"[^\"]+\""), "\"key\": \"[REDACTED]\"")
    }
    
    private fun generateRequestId(): String {
        return "req_${TimeUtils.currentTimeMillis()}_${(0..999).random()}"
    }
}

fun HttpClientConfig<*>.installOpenAILogging(
    logLevel: OpenAILogLevel = OpenAILogLevel.BASIC,
    debugLoggingService: DebugLoggingService? = null,
    loggingManager: LoggingManager? = null
) {
    val interceptor = OpenAILoggingInterceptor(logLevel, debugLoggingService, loggingManager)
    
    install(Logging) {
        level = interceptor.getKtorLogLevel()
        logger = interceptor.createLogger()
        
        sanitizeHeader { name -> 
            name.equals("Authorization", ignoreCase = true) ||
            name.equals("Api-Key", ignoreCase = true) ||
            name.equals("X-API-Key", ignoreCase = true)
        }
    }
    
    // Note: Advanced debug logging would be implemented with custom interceptors
    // For now, the enhanced Logger above provides debug capabilities
}

/**
 * Enhanced debug logging functionality for HTTP clients
 * This provides basic debug logging capabilities that work with the existing Ktor logging system
 */
object DebugHttpLogging {
    
    fun logDebugRequest(url: String, method: String, debugLoggingService: DebugLoggingService?) {
        if (debugLoggingService?.isDebugBuild() == true) {
            println("[DEBUG] HTTP Request: $method $url")
        }
    }
    
    fun logDebugResponse(url: String, statusCode: Int, duration: Long, debugLoggingService: DebugLoggingService?) {
        if (debugLoggingService?.isDebugBuild() == true) {
            println("[DEBUG] HTTP Response: $statusCode for $url (${duration}ms)")
        }
    }
}