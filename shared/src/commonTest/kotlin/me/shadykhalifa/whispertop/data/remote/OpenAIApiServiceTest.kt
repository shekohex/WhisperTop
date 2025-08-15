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

class OpenAIApiServiceTest {

    private fun createMockClient(
        responseContent: String = """{"text": "test transcription"}""",
        statusCode: HttpStatusCode = HttpStatusCode.OK
    ): HttpClient {
        val mockEngine = MockEngine { request ->
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
    fun `transcribe with ByteArray should return successful response`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "Hello world transcription"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1,
            language = "en"
        )
        
        // Then
        assertEquals("Hello world transcription", result.text)
        
        mockClient.close()
    }

    @Test
    fun `transcribe with string model should validate and convert to enum`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "GPT-4o transcription"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = "gpt-4o-transcribe",
            language = "en",
            responseFormat = "json"
        )
        
        // Then
        assertEquals("GPT-4o transcription", result.text)
        
        mockClient.close()
    }

    @Test
    fun `transcribe should throw exception for invalid model`() = runTest {
        // Given
        val mockClient = createMockClient()
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = "invalid-model"
            )
        }
        
        mockClient.close()
    }

    @Test
    fun `transcribe should throw exception for invalid response format`() = runTest {
        // Given
        val mockClient = createMockClient()
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = "whisper-1",
                responseFormat = "invalid-format"
            )
        }
        
        mockClient.close()
    }

    @Test
    fun `transcribe should throw exception for invalid temperature range below zero`() = runTest {
        // Given
        val mockClient = createMockClient()
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = WhisperModel.WHISPER_1,
                temperature = -0.1f
            )
        }
        
        mockClient.close()
    }

    @Test
    fun `transcribe should throw exception for invalid temperature range above one`() = runTest {
        // Given
        val mockClient = createMockClient()
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = WhisperModel.WHISPER_1,
                temperature = 1.1f
            )
        }
        
        mockClient.close()
    }

    @Test
    fun `transcribe should accept valid temperature range boundaries`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "boundary test"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When - Test both boundaries
        val result1 = service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1,
            temperature = 0.0f
        )
        
        val result2 = service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1,
            temperature = 1.0f
        )
        
        // Then
        assertEquals("boundary test", result1.text)
        assertEquals("boundary test", result2.text)
        
        mockClient.close()
    }

    @Test
    fun `transcribe should handle verbose JSON response format`() = runTest {
        // Given
        val verboseResponse = """{
            "text": "verbose transcription",
            "language": "en",
            "duration": 45.5,
            "words": [],
            "segments": []
        }"""
        val mockClient = createMockClient(verboseResponse)
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1,
            responseFormat = AudioResponseFormat.VERBOSE_JSON
        )
        
        // Then
        assertEquals("verbose transcription", result.text)
        
        mockClient.close()
    }

    @Test
    fun `transcribe should determine correct content type for different file extensions`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "file type test"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When/Then - Test different file extensions (all should succeed)
        val fileTypes = listOf(
            "test.wav",
            "test.mp3", 
            "test.m4a",
            "test.ogg",
            "test.webm",
            "test.flac",
            "test.unknown"
        )
        
        fileTypes.forEach { fileName ->
            val result = service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = WhisperModel.WHISPER_1
            )
            assertEquals("file type test", result.text)
        }
        
        mockClient.close()
    }

    @Test
    fun `transcribe should handle all optional parameters`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "full parameters test"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When
        val result = service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.GPT_4O_TRANSCRIBE,
            language = "es",
            prompt = "This is Spanish audio",
            responseFormat = AudioResponseFormat.TEXT,
            temperature = 0.5f
        )
        
        // Then
        assertEquals("full parameters test", result.text)
        
        mockClient.close()
    }

    @Test
    fun `transcribe should throw OpenAIException on HTTP error`() = runTest {
        // Given
        val mockClient = createMockClient(
            responseContent = """{"error": {"message": "API key invalid", "type": "invalid_request_error"}}""",
            statusCode = HttpStatusCode.Unauthorized
        )
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then
        val exception = assertFailsWith<OpenAIException.AuthenticationException> {
            service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = WhisperModel.WHISPER_1
            )
        }
        
        assertEquals(401, exception.statusCode)
        
        mockClient.close()
    }

    @Test
    fun `transcribe should handle server error status codes`() = runTest {
        // Given
        val mockClient = createMockClient(
            responseContent = """{"error": {"message": "Internal server error", "type": "server_error"}}""",
            statusCode = HttpStatusCode.InternalServerError
        )
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then
        val exception = assertFailsWith<OpenAIException.ServerException> {
            service.transcribe(
                audioData = audioData,
                fileName = fileName,
                model = WhisperModel.WHISPER_1
            )
        }
        
        assertEquals(500, exception.statusCode)
        
        mockClient.close()
    }

    @Test
    fun `createOpenAIApiService should create service with proper configuration`() {
        // Given
        val apiKey = "test-api-key"
        val baseUrl = "https://custom-api.example.com/v1/"
        
        // When
        val service = createOpenAIApiService(apiKey, baseUrl)
        
        // Then
        assertNotNull(service)
    }

    @Test
    fun `createOpenAIApiService should use default base URL when not provided`() {
        // Given
        val apiKey = "test-api-key"
        
        // When
        val service = createOpenAIApiService(apiKey)
        
        // Then
        assertNotNull(service)
    }

    @Test
    fun `validateParameters should allow boundary temperature values`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "boundary validation"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When/Then - Should not throw exceptions
        service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1,
            temperature = 0.0f
        )
        
        service.transcribe(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1,
            temperature = 1.0f
        )
        
        mockClient.close()
    }

    @Test
    fun `determineContentType should handle case insensitive file extensions`() = runTest {
        // Given
        val mockClient = createMockClient("""{"text": "case test"}""")
        val service = OpenAIApiService(mockClient)
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        
        // When/Then - Should handle uppercase extensions
        val result1 = service.transcribe(
            audioData = audioData,
            fileName = "test.WAV",
            model = WhisperModel.WHISPER_1
        )
        
        val result2 = service.transcribe(
            audioData = audioData,
            fileName = "test.MP3",
            model = WhisperModel.WHISPER_1
        )
        
        // Then
        assertEquals("case test", result1.text)
        assertEquals("case test", result2.text)
        
        mockClient.close()
    }
}