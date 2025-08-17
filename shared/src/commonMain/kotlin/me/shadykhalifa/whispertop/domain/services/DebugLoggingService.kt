package me.shadykhalifa.whispertop.domain.services

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

interface DebugLoggingService {
    
    /**
     * Check if debug logging is enabled for the current build
     */
    fun isDebugBuild(): Boolean
    
    /**
     * Log detailed HTTP request information
     */
    suspend fun logRequest(
        request: HttpRequestBuilder,
        requestId: String,
        timestamp: Long
    )
    
    /**
     * Log detailed HTTP response information
     */
    suspend fun logResponse(
        response: HttpResponse,
        requestId: String,
        timestamp: Long,
        duration: Long
    )
    
    /**
     * Log network error with context
     */
    suspend fun logNetworkError(
        error: Throwable,
        requestId: String,
        timestamp: Long,
        context: String
    )
    
    /**
     * Log retry attempt with details
     */
    suspend fun logRetryAttempt(
        attempt: Int,
        maxAttempts: Int,
        requestId: String,
        reason: String,
        delayMs: Long
    )
    
    /**
     * Log connection pool metrics
     */
    suspend fun logConnectionMetrics(
        activeConnections: Int,
        idleConnections: Int,
        queuedRequests: Int
    )
    
    /**
     * Log request timing breakdown
     */
    suspend fun logRequestTiming(
        requestId: String,
        dnsLookupTime: Long,
        connectionTime: Long,
        sslHandshakeTime: Long,
        requestTime: Long,
        responseTime: Long,
        totalTime: Long
    )
}