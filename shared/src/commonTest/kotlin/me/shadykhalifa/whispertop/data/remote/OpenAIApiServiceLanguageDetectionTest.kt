package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.shadykhalifa.whispertop.data.models.WhisperModel

class OpenAIApiServiceLanguageDetectionTest {

    private fun createMockClient(responseBody: String, statusCode: HttpStatusCode = HttpStatusCode.OK): HttpClient {
        return HttpClient(MockEngine) {
            engine {
                addHandler { request ->
                    respond(
                        content = responseBody,
                        status = statusCode,
                        headers = headersOf(HttpHeaders.ContentType, "application/json")
                    )
                }
            }
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
    fun `transcribeWithLanguageDetection should return verbose response with language info`() = runTest {
        val responseBody = """
            {
                "text": "Hello world, this is a test.",
                "language": "en",
                "duration": 2.5,
                "words": [
                    {"word": "Hello", "start": 0.0, "end": 0.5},
                    {"word": "world", "start": 0.6, "end": 1.0}
                ],
                "segments": [
                    {
                        "id": 0,
                        "seek": 0,
                        "start": 0.0,
                        "end": 2.5,
                        "text": "Hello world, this is a test.",
                        "tokens": [1234, 5678],
                        "temperature": 0.0,
                        "avg_logprob": -0.3,
                        "compression_ratio": 1.2,
                        "no_speech_prob": 0.01
                    }
                ]
            }
        """.trimIndent()

        val mockClient = createMockClient(responseBody)
        val service = OpenAIApiService(mockClient)
        
        val audioData = "fake audio data".toByteArray()
        val fileName = "test.wav"

        val result = service.transcribeWithLanguageDetection(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.GPT_4O_TRANSCRIBE,
            language = null, // Auto-detect
            temperature = 0.0f
        )

        assertEquals("Hello world, this is a test.", result.text)
        assertEquals("en", result.language)
        assertEquals(2.5f, result.duration)
        assertEquals(2, result.words?.size)
        assertEquals(1, result.segments?.size)
    }

    @Test
    fun `transcribeWithLanguageDetection should work with manual language override`() = runTest {
        val responseBody = """
            {
                "text": "Hola mundo, esto es una prueba.",
                "language": "es",
                "duration": 3.0
            }
        """.trimIndent()

        val mockClient = createMockClient(responseBody)
        val service = OpenAIApiService(mockClient)
        
        val audioData = "fake audio data".toByteArray()
        val fileName = "test.wav"

        val result = service.transcribeWithLanguageDetection(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.GPT_4O_MINI_TRANSCRIBE,
            language = "es", // Manual override
            temperature = 0.0f
        )

        assertEquals("Hola mundo, esto es una prueba.", result.text)
        assertEquals("es", result.language)
        assertEquals(3.0f, result.duration)
    }

    @Test
    fun `transcribeWithLanguageDetection should use GPT_4O_TRANSCRIBE as default model`() = runTest {
        val responseBody = """
            {
                "text": "Test transcription.",
                "language": "en",
                "duration": 1.0
            }
        """.trimIndent()

        val mockClient = createMockClient(responseBody)
        val service = OpenAIApiService(mockClient)
        
        val audioData = "fake audio data".toByteArray()
        val fileName = "test.wav"

        // Not specifying model should use default
        val result = service.transcribeWithLanguageDetection(
            audioData = audioData,
            fileName = fileName
        )

        assertEquals("Test transcription.", result.text)
        assertEquals("en", result.language)
    }

    @Test
    fun `transcribeWithLanguageDetection should handle responses without words and segments`() = runTest {
        val responseBody = """
            {
                "text": "Simple transcription.",
                "language": "fr",
                "duration": 1.5
            }
        """.trimIndent()

        val mockClient = createMockClient(responseBody)
        val service = OpenAIApiService(mockClient)
        
        val audioData = "fake audio data".toByteArray()
        val fileName = "test.wav"

        val result = service.transcribeWithLanguageDetection(
            audioData = audioData,
            fileName = fileName,
            model = WhisperModel.WHISPER_1
        )

        assertEquals("Simple transcription.", result.text)
        assertEquals("fr", result.language)
        assertEquals(1.5f, result.duration)
        assertEquals(null, result.words)
        assertEquals(null, result.segments)
    }

    @Test
    fun `transcribeWithLanguageDetection should validate temperature parameter`() = runTest {
        val responseBody = """
            {
                "text": "Test",
                "language": "en",
                "duration": 1.0
            }
        """.trimIndent()

        val mockClient = createMockClient(responseBody)
        val service = OpenAIApiService(mockClient)
        
        val audioData = "fake audio data".toByteArray()
        val fileName = "test.wav"

        // Test that temperature validation works
        try {
            service.transcribeWithLanguageDetection(
                audioData = audioData,
                fileName = fileName,
                temperature = 1.5f // Invalid temperature > 1.0
            )
            assertTrue(false, "Should have thrown exception for invalid temperature")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Temperature") == true)
        }

        try {
            service.transcribeWithLanguageDetection(
                audioData = audioData,
                fileName = fileName,
                temperature = -0.1f // Invalid temperature < 0.0
            )
            assertTrue(false, "Should have thrown exception for invalid temperature")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message?.contains("Temperature") == true)
        }
    }

    @Test
    fun `transcribeWithLanguageDetection should use verbose_json response format`() = runTest {
        val responseBody = """
            {
                "text": "Test transcription with verbose format.",
                "language": "en",
                "duration": 2.0,
                "words": [],
                "segments": []
            }
        """.trimIndent()

        val mockClient = createMockClient(responseBody)
        val service = OpenAIApiService(mockClient)
        
        val audioData = "fake audio data".toByteArray()
        val fileName = "test.wav"

        val result = service.transcribeWithLanguageDetection(
            audioData = audioData,
            fileName = fileName
        )

        // Verify that we get verbose response format fields
        assertEquals("Test transcription with verbose format.", result.text)
        assertEquals("en", result.language)
        assertEquals(2.0f, result.duration)
        // Even if empty, these fields should be present in verbose format
        assertTrue(result.words != null)
        assertTrue(result.segments != null)
    }
}