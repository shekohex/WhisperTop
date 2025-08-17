package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.*
import kotlin.test.*

class CustomEndpointTest {

    private fun createMockClient(
        responseContent: String = """{"text": "custom endpoint response"}""",
        statusCode: HttpStatusCode = HttpStatusCode.OK,
        requestValidator: ((MockRequestHandleScope) -> Unit)? = null
    ): HttpClient {
        val mockEngine = MockEngine { request ->
            requestValidator?.invoke(this)
            respond(
                content = responseContent,
                status = statusCode,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        
        return HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    encodeDefaults = false
                })
            }
            installErrorHandling()
        }
    }

    @Test
    fun `should handle custom models from alternative providers`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Response from Hugging Face Whisper"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "openai/whisper-large-v3"
        )
        
        // Then
        assertEquals("Response from Hugging Face Whisper", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle local model deployments`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Local Whisper model response"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "whisper-local-deployment",
            language = "fr",
            prompt = "French audio transcription"
        )
        
        // Then
        assertEquals("Local Whisper model response", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle Groq API compatible endpoints`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Groq Whisper response"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "whisper-large-v3",  // Groq's model name
            responseFormat = "json"
        )
        
        // Then
        assertEquals("Groq Whisper response", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle Azure OpenAI custom deployments`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Azure OpenAI custom deployment"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "my-whisper-deployment"  // Azure deployment name
        )
        
        // Then
        assertEquals("Azure OpenAI custom deployment", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should maintain backward compatibility with OpenAI models`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Official OpenAI response"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When - Use official OpenAI model
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "whisper-1"  // Should use enum path
        )
        
        // Then
        assertEquals("Official OpenAI response", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle custom endpoint errors gracefully`() = runTest {
        // Given
        val mockClient = createMockClient(
            responseContent = """{"error": {"message": "Custom endpoint rate limit", "type": "rate_limit_error"}}""",
            statusCode = HttpStatusCode.TooManyRequests
        )
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When/Then
        val exception = assertFailsWith<OpenAIException.RateLimitException> {
            service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "custom-whisper-model"
            )
        }
        
        assertEquals(429, exception.statusCode)
        
        mockClient.close()
    }

    @Test
    fun `should handle different JSON response formats for custom models`() = runTest {
        // Given - Test JSON formats that our implementation supports
        val formats = mapOf(
            "json" to """{"text": "JSON format"}""",
            "verbose_json" to """{"text": "Verbose JSON", "language": "en", "duration": 1.5}"""
        )
        
        for ((format, expectedResponse) in formats) {
            val mockClient = createMockClient(expectedResponse)
            val service = OpenAIApiService(mockClient)
            val audioData = byteArrayOf(1, 2, 3, 4, 5)
            
            // When
            val result = service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "custom-model",
                responseFormat = format
            )
            
            // Then
            when (format) {
                "json" -> assertEquals("JSON format", result.text)
                "verbose_json" -> assertEquals("Verbose JSON", result.text)
            }
            
            mockClient.close()
        }
    }

    @Test
    fun `should validate temperature for custom models same as built-in`() = runTest {
        // Given
        val mockClient = createMockClient()
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When/Then - Should allow valid range
        val validResult = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "custom-model",
            temperature = 0.7f
        )
        assertNotNull(validResult)
        
        // Should reject invalid range
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "custom-model",
                temperature = 1.5f
            )
        }
        
        mockClient.close()
    }
}