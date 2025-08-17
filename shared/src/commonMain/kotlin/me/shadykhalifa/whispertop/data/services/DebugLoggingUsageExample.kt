package me.shadykhalifa.whispertop.data.services

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.shadykhalifa.whispertop.data.remote.createDebugHttpClient
import me.shadykhalifa.whispertop.domain.services.DebugLoggingService
import me.shadykhalifa.whispertop.domain.services.LoggingManager

/**
 * Example usage of the debug logging system for WhisperTop.
 * This demonstrates how API requests/responses are logged in debug mode
 * with comprehensive sanitization and performance tracking.
 */
class DebugLoggingUsageExample(
    private val debugLoggingService: DebugLoggingService,
    private val loggingManager: LoggingManager
) {
    
    suspend fun demonstrateDebugAPILogging() {
        println("=== Debug API Logging Demonstration ===\n")
        
        if (!debugLoggingService.isDebugBuild()) {
            println("Debug logging is only enabled in debug builds.")
            println("Current build is: ${if (debugLoggingService.isDebugBuild()) "DEBUG" else "RELEASE"}")
            return
        }
        
        // Create a debug-enabled HTTP client
        val debugClient = createDebugHttpClient(
            debugLoggingService = debugLoggingService,
            loggingManager = loggingManager,
            enableDebugMode = true
        )
        
        try {
            // Demonstrate API request logging with real OpenAI-like request
            demonstrateTranscriptionRequestLogging(debugClient)
            
            // Demonstrate error handling and retry logging
            demonstrateErrorLogging(debugClient)
            
            // Demonstrate connection metrics logging
            demonstrateConnectionMetrics()
            
        } finally {
            debugClient.close()
        }
        
        println("\n=== Debug Logging Demo Complete ===")
    }
    
    private suspend fun demonstrateTranscriptionRequestLogging(client: HttpClient) {
        println("--- Transcription Request Logging ---")
        
        try {
            // This will be logged with full debug details
            val response = client.post("https://api.openai.com/v1/audio/transcriptions") {
                headers {
                    append("Authorization", "Bearer sk-fake-api-key-for-demo-purposes-only")
                    append("Content-Type", "multipart/form-data")
                }
                
                // Simulate multipart form data
                setBody("--boundary\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\nwhisper-1\r\n--boundary--")
            }
            
            println("Response status: ${response.status}")
            
        } catch (e: Exception) {
            println("Expected error for demo API call: ${e.message}")
        }
    }
    
    private suspend fun demonstrateErrorLogging(client: HttpClient) {
        println("\n--- Error Handling and Retry Logging ---")
        
        try {
            // This will trigger retry logic and error logging
            val response = client.get("https://httpstat.us/500") {
                headers {
                    append("Authorization", "Bearer sk-another-fake-key")
                }
            }
            
        } catch (e: Exception) {
            println("Expected error for retry demo: ${e.message}")
        }
    }
    
    private suspend fun demonstrateConnectionMetrics() {
        println("\n--- Connection Metrics Logging ---")
        
        // Log simulated connection pool metrics
        debugLoggingService.logConnectionMetrics(
            activeConnections = 3,
            idleConnections = 7,
            queuedRequests = 1
        )
        
        // Log request timing breakdown
        debugLoggingService.logRequestTiming(
            requestId = "demo-request-123",
            dnsLookupTime = 50,
            connectionTime = 150,
            sslHandshakeTime = 200,
            requestTime = 100,
            responseTime = 2000,
            totalTime = 2500
        )
    }
    
    suspend fun demonstrateSanitization() {
        println("=== Sanitization Demonstration ===\n")
        
        val testMessages = listOf(
            "Authorization: Bearer sk-1234567890abcdef1234567890abcdef",
            "API request with api_key=sk-sensitive-key-here",
            "{\"api_key\": \"sk-very-secret-key\", \"model\": \"whisper-1\"}",
            "Bearer token: sk-proj-abcdefghijklmnopqrstuvwxyz123456",
            "Normal log message without sensitive data",
            "User uploaded file: audio.wav (1.2MB)"
        )
        
        println("Original → Sanitized:")
        testMessages.forEach { message ->
            val sanitized = sanitizeForDemo(message)
            println("$message → $sanitized")
        }
        
        println("\n=== Sanitization Demo Complete ===")
    }
    
    private fun sanitizeForDemo(message: String): String {
        return message
            .replace(Regex("Bearer [A-Za-z0-9-._~+/]+=*"), "Bearer [REDACTED]")
            .replace(Regex("sk-[A-Za-z0-9]{32,}"), "sk-[REDACTED]")
            .replace(Regex("\"api_key\"\\s*:\\s*\"[^\"]+\""), "\"api_key\": \"[REDACTED]\"")
            .replace(Regex("api_key=[^&\\s]+"), "api_key=[REDACTED]")
    }
    
    suspend fun demonstrateDebugModeFeatures() {
        println("=== Debug Mode Features ===\n")
        
        println("Debug Build Status: ${debugLoggingService.isDebugBuild()}")
        
        if (debugLoggingService.isDebugBuild()) {
            println("✅ Debug logging is ENABLED")
            println("Features available:")
            println("  • Detailed request/response logging")
            println("  • Request timing breakdown")
            println("  • Connection pool monitoring")
            println("  • Automatic retry logging")
            println("  • Enhanced error context")
            println("  • JSON pretty-printing")
            println("  • Extended timeout values")
        } else {
            println("❌ Debug logging is DISABLED")
            println("Features NOT available in release builds:")
            println("  • No sensitive data logging")
            println("  • Basic error logging only")
            println("  • No request/response details")
            println("  • Standard timeout values")
        }
        
        println("\n=== Debug Mode Demo Complete ===")
    }
    
    suspend fun runFullDemo() {
        demonstrateDebugModeFeatures()
        println()
        
        demonstrateSanitization()
        println()
        
        demonstrateDebugAPILogging()
    }
}