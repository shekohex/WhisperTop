package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

class HttpClientProvider(
    private val baseUrl: String = "https://api.openai.com/v1/",
    private val apiKey: String? = null,
    private val logLevel: OpenAILogLevel = OpenAILogLevel.BASIC
) {
    fun createClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                })
            }
            
            installOpenAILogging(logLevel)
            installErrorHandling()
            
            install(DefaultRequest) {
                url(baseUrl)
                headers.append("Content-Type", "application/json")
                apiKey?.let { 
                    headers.append("Authorization", "Bearer $it") 
                }
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
        }
    }
}