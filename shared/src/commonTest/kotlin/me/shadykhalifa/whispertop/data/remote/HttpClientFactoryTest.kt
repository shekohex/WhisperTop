package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import kotlin.test.*

class HttpClientFactoryTest {

    @Test
    fun `createHttpClient should create client with ContentNegotiation plugin`() {
        // Given & When
        val client = createHttpClient()
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(ContentNegotiation) != null)
    }

    @Test
    fun `createHttpClient should create client with Logging plugin`() {
        // Given & When
        val client = createHttpClient()
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(Logging) != null)
    }

    @Test
    fun `createHttpClient should create client with DefaultRequest plugin`() {
        // Given & When
        val client = createHttpClient()
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(DefaultRequest) != null)
    }

    @Test
    fun `createHttpClient should create client with HttpTimeout plugin`() {
        // Given & When
        val client = createHttpClient()
        
        // Then
        assertNotNull(client)
        assertTrue(client.pluginOrNull(HttpTimeout) != null)
    }

    @Test
    fun `createHttpClient should be non-null and properly configured`() {
        // Given & When
        val client = createHttpClient()
        
        // Then
        assertNotNull(client)
        // Verify client can be closed without errors
        client.close()
    }
}