package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import kotlin.test.*

/**
 * Test class to verify that all required Ktor dependencies for OpenAI API client
 * are properly available and can be imported.
 */
class KtorDependenciesTest {

    @Test
    fun `ktor-client-core dependency should be available`() {
        // Given & When
        val client = HttpClient()
        
        // Then
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `ktor-client-content-negotiation dependency should be available`() {
        // Given & When
        val client = HttpClient {
            install(ContentNegotiation) {
                json()
            }
        }
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(ContentNegotiation) != null)
        client.close()
    }

    @Test
    fun `ktor-serialization-kotlinx-json dependency should be available`() {
        // Given & When
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
        
        // Then
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `ktor-client-logging dependency should be available`() {
        // Given & When
        val client = HttpClient {
            install(Logging) {
                level = LogLevel.INFO
            }
        }
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(Logging) != null)
        client.close()
    }

    @Test
    fun `kotlinx-serialization-json dependency should be available`() {
        // Given & When
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        
        // Then
        assertNotNull(json)
        assertTrue(json.configuration.ignoreUnknownKeys)
        assertTrue(json.configuration.isLenient)
    }

    @Test
    fun `all plugins can be installed together`() {
        // Given & When
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
            
            install(Logging) {
                level = LogLevel.INFO
            }
            
            install(DefaultRequest) {
                headers.append("User-Agent", "WhisperTop/1.0")
            }
            
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
                connectTimeoutMillis = 10_000
            }
        }
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(ContentNegotiation) != null)
        assertTrue(client.pluginOrNull(Logging) != null)
        assertTrue(client.pluginOrNull(DefaultRequest) != null)
        assertTrue(client.pluginOrNull(HttpTimeout) != null)
        client.close()
    }
}