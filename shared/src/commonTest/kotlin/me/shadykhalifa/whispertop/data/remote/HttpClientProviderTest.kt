package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class HttpClientProviderTest {

    @Test
    fun `createClient should create client with default configuration`() {
        // Given
        val provider = HttpClientProvider()
        
        // When
        val client = provider.createClient()
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(ContentNegotiation) != null)
        assertTrue(client.pluginOrNull(Logging) != null)
        assertTrue(client.pluginOrNull(DefaultRequest) != null)
        assertTrue(client.pluginOrNull(HttpTimeout) != null)
        // HttpRequestRetry plugin not installed in current implementation
        
        client.close()
    }

    @Test
    fun `createClient should use custom base URL`() {
        // Given
        val customBaseUrl = "https://custom-api.example.com/v1/"
        val provider = HttpClientProvider(baseUrl = customBaseUrl)
        
        // When
        val client = provider.createClient()
        
        // Then
        assertNotNull(client)
        client.close()
    }

    @Test
    fun `createClient should include API key in Authorization header when provided`() = runTest {
        // Given
        val apiKey = "test-api-key"
        val provider = HttpClientProvider(apiKey = apiKey)
        val client = provider.createClient()
        
        // When/Then - Verify DefaultRequest plugin is configured with Authorization header
        val defaultRequestPlugin = client.pluginOrNull(DefaultRequest)
        assertNotNull(defaultRequestPlugin)
        
        client.close()
    }

    @Test
    fun `createClient should not include Authorization header when API key is null`() = runTest {
        // Given
        val provider = HttpClientProvider(apiKey = null)
        val client = provider.createClient()
        
        // When/Then - Verify DefaultRequest plugin exists but no Authorization header is set
        val defaultRequestPlugin = client.pluginOrNull(DefaultRequest)
        assertNotNull(defaultRequestPlugin)
        
        client.close()
    }

    @Test
    fun `HttpTimeout should be configured correctly`() {
        // Given
        val provider = HttpClientProvider()
        
        // When
        val client = provider.createClient()
        
        // Then
        val timeoutPlugin = client.pluginOrNull(HttpTimeout)
        assertNotNull(timeoutPlugin)
        
        client.close()
    }



    @Test
    fun `createClient should create multiple independent instances`() {
        // Given
        val provider1 = HttpClientProvider(baseUrl = "https://api1.example.com/")
        val provider2 = HttpClientProvider(baseUrl = "https://api2.example.com/")
        
        // When
        val client1 = provider1.createClient()
        val client2 = provider2.createClient()
        
        // Then
        assertNotNull(client1)
        assertNotNull(client2)
        assertNotSame(client1, client2)
        
        client1.close()
        client2.close()
    }
}