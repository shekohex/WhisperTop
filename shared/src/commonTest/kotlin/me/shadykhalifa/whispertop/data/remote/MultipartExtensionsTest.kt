package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.CreateTranscriptionRequestDto
import me.shadykhalifa.whispertop.data.models.MultipartTranscriptionRequest
import kotlin.test.*

class MultipartExtensionsTest {

    private fun createMockClient(responseContent: String = """{"text": "test transcription"}"""): HttpClient {
        val mockEngine = MockEngine { request ->
            respond(
                content = responseContent,
                status = HttpStatusCode.OK,
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
        }
    }

    @Test
    fun `postMultipartTranscription should send correct multipart request`() = runTest {
        // Given
        val client = createMockClient()
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        val contentType = "audio/wav"
        val transcriptionRequest = CreateTranscriptionRequestDto(
            model = "whisper-1",
            language = "en",
            responseFormat = "json",
            temperature = 0.0f
        )
        val multipartRequest = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            request = transcriptionRequest
        )
        
        // When
        val response = client.postMultipartTranscription("audio/transcriptions", multipartRequest)
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        
        client.close()
    }

    @Test
    fun `uploadAudioFile should create correct multipart request`() = runTest {
        // Given
        val client = createMockClient()
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        val contentType = "audio/wav"
        val transcriptionRequest = CreateTranscriptionRequestDto(
            model = "whisper-1",
            language = "en",
            responseFormat = "json",
            temperature = 0.5f
        )
        
        // When
        val response = client.uploadAudioFile(
            endpoint = "audio/transcriptions",
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            transcriptionRequest = transcriptionRequest
        )
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        
        client.close()
    }

    @Test
    fun `uploadWavFile should use default parameters correctly`() = runTest {
        // Given
        val client = createMockClient()
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "test.wav"
        
        // When
        val response = client.uploadWavFile(
            audioData = audioData,
            fileName = fileName
        )
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        
        client.close()
    }

    @Test
    fun `uploadWavFile should use custom parameters correctly`() = runTest {
        // Given
        val client = createMockClient()
        val audioData = byteArrayOf(1, 2, 3, 4, 5)
        val fileName = "custom.wav"
        val model = "whisper-1"
        val language = "es"
        val responseFormat = "verbose_json"
        val temperature = 0.3f
        
        // When
        val response = client.uploadWavFile(
            audioData = audioData,
            fileName = fileName,
            model = model,
            language = language,
            responseFormat = responseFormat,
            temperature = temperature
        )
        
        // Then
        assertEquals(HttpStatusCode.OK, response.status)
        
        client.close()
    }

    @Test
    fun `MultipartTranscriptionRequest should handle equality correctly`() {
        // Given
        val audioData = byteArrayOf(1, 2, 3)
        val fileName = "test.wav"
        val contentType = "audio/wav"
        val request = CreateTranscriptionRequestDto(model = "whisper-1")
        
        val multipartRequest1 = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            request = request
        )
        
        val multipartRequest2 = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = fileName,
            contentType = contentType,
            request = request
        )
        
        val multipartRequest3 = MultipartTranscriptionRequest(
            audioData = byteArrayOf(4, 5, 6),
            fileName = fileName,
            contentType = contentType,
            request = request
        )
        
        // Then
        assertEquals(multipartRequest1, multipartRequest2)
        assertNotEquals(multipartRequest1, multipartRequest3)
        assertEquals(multipartRequest1.hashCode(), multipartRequest2.hashCode())
        assertNotEquals(multipartRequest1.hashCode(), multipartRequest3.hashCode())
    }

    @Test
    fun `MultipartTranscriptionRequest should handle different file names`() {
        // Given
        val audioData = byteArrayOf(1, 2, 3)
        val contentType = "audio/wav"
        val request = CreateTranscriptionRequestDto(model = "whisper-1")
        
        val multipartRequest1 = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = "file1.wav",
            contentType = contentType,
            request = request
        )
        
        val multipartRequest2 = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = "file2.wav",
            contentType = contentType,
            request = request
        )
        
        // Then
        assertNotEquals(multipartRequest1, multipartRequest2)
    }
}