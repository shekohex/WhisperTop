package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*

enum class OpenAILogLevel {
    NONE,
    BASIC,
    HEADERS,
    BODY
}

class OpenAILoggingInterceptor(
    private val logLevel: OpenAILogLevel = OpenAILogLevel.BASIC
) {
    
    fun getKtorLogLevel(): LogLevel {
        return when (logLevel) {
            OpenAILogLevel.NONE -> LogLevel.NONE
            OpenAILogLevel.BASIC -> LogLevel.INFO
            OpenAILogLevel.HEADERS -> LogLevel.HEADERS
            OpenAILogLevel.BODY -> LogLevel.ALL
        }
    }
    
    fun createLogger(): Logger {
        return object : Logger {
            override fun log(message: String) {
                when (logLevel) {
                    OpenAILogLevel.NONE -> {}
                    else -> {
                        val sanitizedMessage = sanitizeLogMessage(message)
                        println("[OpenAI-API] $sanitizedMessage")
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
    }
}

fun HttpClientConfig<*>.installOpenAILogging(logLevel: OpenAILogLevel = OpenAILogLevel.BASIC) {
    val interceptor = OpenAILoggingInterceptor(logLevel)
    
    install(Logging) {
        level = interceptor.getKtorLogLevel()
        logger = interceptor.createLogger()
        
        sanitizeHeader { name -> 
            name.equals("Authorization", ignoreCase = true) ||
            name.equals("Api-Key", ignoreCase = true)
        }
    }
}