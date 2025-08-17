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

class CustomModelApiTest {

    private fun createMockClient(
        responseContent: String = """{"text": "Test transcription"}""",
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        val mockEngine = MockEngine { 
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
    fun `should handle custom model transcription successfully`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Custom model response"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "custom-whisper-model"
        )
        
        // Then
        assertEquals("Custom model response", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle Groq API models`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Groq transcription"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "whisper-large-v3"
        )
        
        // Then
        assertEquals("Groq transcription", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle local deployment models`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Local deployment response"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "whisper-local-deployment"
        )
        
        // Then
        assertEquals("Local deployment response", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should preserve all parameters for custom models`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Custom transcription"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When - Should not throw exceptions with all parameters
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "custom-model",
            language = "fr",
            prompt = "French transcription",
            responseFormat = "json",
            temperature = 0.7f
        )
        
        // Then
        assertEquals("Custom transcription", result.text)
        
        mockClient.close()
    }

    @Test
    fun `should handle error responses from custom endpoints`() = runTest {
        // Given
        val mockClient = createMockClient(
            responseContent = """{"error": {"message": "Model not found", "type": "invalid_request_error"}}""",
            statusCode = HttpStatusCode.BadRequest
        )
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When/Then
        assertFailsWith<OpenAIException> {
            service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "invalid-custom-model"
            )
        }
        
        mockClient.close()
    }

    @Test
    fun `should handle different JSON response formats`() = runTest {
        // Given - Test different response formats
        val responseFormats = listOf(
            """{"text": "Standard format"}""",
            """{"text": "Verbose format", "language": "en", "duration": 2.5}""",
            """{"text": "Extended format", "language": "en", "duration": 3.0, "segments": []}"""
        )
        
        responseFormats.forEachIndexed { index, response ->
            val mockClient = createMockClient(response)
            val service = OpenAIApiService(mockClient)
            val audioData = byteArrayOf(1, 2, 3, 4, 5)
            
            // When
            val result = service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "custom-model-$index"
            )
            
            // Then - Should extract text from any format
            assertTrue(result.text.isNotEmpty())
            assertTrue(result.text.contains("format"))
            
            mockClient.close()
        }
    }

    @Test
    fun `should validate temperature parameter`() = runTest {
        // Given
        val mockClient = createMockClient()
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When/Then - Invalid temperature should be rejected
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "custom-model",
                temperature = 1.5f
            )
        }
        
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = "test.wav",
                model = "custom-model",
                temperature = -0.1f
            )
        }
        
        mockClient.close()
    }

    @Test
    fun `should maintain backward compatibility with OpenAI models`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "OpenAI response"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When - Use official OpenAI model
        val result = service.transcribe(
            audioData = audioData,
            fileName = "test.wav",
            model = "whisper-1"
        )
        
        // Then
        assertEquals("OpenAI response", result.text)
        
        mockClient.close()
    }
}