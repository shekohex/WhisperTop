package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.domain.services.DebugLoggingService
import me.shadykhalifa.whispertop.domain.services.LoggingManager

fun createHttpClient(): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
            })
        }
        
        install(Logging) {
            level = LogLevel.INFO
        }
        
        install(DefaultRequest) {
            headers.append("Content-Type", "application/json")
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 60_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 60_000
        }
    }
}

/**
 * Creates an HTTP client with enhanced debug logging capabilities
 */
fun createDebugHttpClient(
    debugLoggingService: DebugLoggingService? = null,
    loggingManager: LoggingManager? = null,
    enableDebugMode: Boolean = false
): HttpClient {
    return HttpClient {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = false
                prettyPrint = enableDebugMode
            })
        }
        
        // Install enhanced OpenAI logging
        installOpenAILogging(
            logLevel = if (enableDebugMode) OpenAILogLevel.DEBUG else OpenAILogLevel.BASIC,
            debugLoggingService = debugLoggingService,
            loggingManager = loggingManager
        )
        
        // Install retry logic
        install(HttpRequestRetry) {
            maxRetries = 3
            retryIf { _, response ->
                response.status.value >= 500
            }
            delayMillis { retry ->
                1000L * retry
            }
        }
        
        install(DefaultRequest) {
            headers.append("Content-Type", "application/json")
            headers.append("User-Agent", "WhisperTop-Android/1.0")
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = if (enableDebugMode) 120_000 else 60_000 // Longer timeout for debug
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = if (enableDebugMode) 120_000 else 60_000
        }
    }
}