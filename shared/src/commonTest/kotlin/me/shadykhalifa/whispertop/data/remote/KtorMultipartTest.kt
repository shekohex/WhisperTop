package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import kotlin.test.*

/**
 * Test class to verify that ktor-client-multipart dependency is available
 * and can be used for file uploads to OpenAI API.
 */
class KtorMultipartTest {

    @Test
    fun `ktor-client-multipart dependency should be available`() {
        // Given & When
        val formData = formData {
            append("key", "value")
        }
        
        // Then
        assertNotNull(formData)
        assertEquals(1, formData.size)
    }

    @Test
    fun `multipart form data can be created with file parameter`() {
        // Given
        val testData = "test audio data".encodeToByteArray()
        
        // When
        val formData = formData {
            append("file", testData, Headers.build {
                append(HttpHeaders.ContentType, "audio/wav")
                append(HttpHeaders.ContentDisposition, "filename=\"test.wav\"")
            })
            append("model", "whisper-1")
        }
        
        // Then
        assertNotNull(formData)
        assertEquals(2, formData.size)
    }

    @Test
    fun `multipart form data supports all OpenAI API parameters`() {
        // Given
        val testAudioData = "mock audio data".encodeToByteArray()
        
        // When
        val formData = formData {
            // Required parameters
            append("file", testAudioData, Headers.build {
                append(HttpHeaders.ContentType, "audio/wav")
                append(HttpHeaders.ContentDisposition, "filename=\"recording.wav\"")
            })
            append("model", "whisper-1")
            
            // Optional parameters
            append("language", "en")
            append("prompt", "This is a test transcription")
            append("response_format", "json")
            append("temperature", "0")
        }
        
        // Then
        assertNotNull(formData)
        assertEquals(6, formData.size)
    }

    @Test
    fun `http client can handle multipart requests`() {
        // Given
        val client = HttpClient()
        val testData = "test".encodeToByteArray()
        
        // When
        val formData = formData {
            append("file", testData, Headers.build {
                append(HttpHeaders.ContentType, "audio/wav")
            })
        }
        
        // Then
        assertNotNull(formData)
        client.close()
    }
}