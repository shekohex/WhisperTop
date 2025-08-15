package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class OpenAITranscriptionDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun createTranscriptionRequestDto_serialization() {
        val request = CreateTranscriptionRequestDto(
            model = "whisper-1",
            language = "en",
            prompt = "Test prompt",
            responseFormat = "json",
            temperature = 0.5f
        )

        val jsonString = json.encodeToString(CreateTranscriptionRequestDto.serializer(), request)
        val deserialized = json.decodeFromString(CreateTranscriptionRequestDto.serializer(), jsonString)

        assertEquals(request, deserialized)
    }

    @Test
    fun createTranscriptionRequestDto_defaultValues() {
        val request = CreateTranscriptionRequestDto(model = "whisper-1")

        assertEquals("whisper-1", request.model)
        assertEquals(null, request.language)
        assertEquals(null, request.prompt)
        assertEquals("json", request.responseFormat)
        assertEquals(0.0f, request.temperature)
    }

    @Test
    fun createTranscriptionResponseDto_serialization() {
        val response = CreateTranscriptionResponseDto(text = "Hello world")
        
        val jsonString = json.encodeToString(CreateTranscriptionResponseDto.serializer(), response)
        val deserialized = json.decodeFromString(CreateTranscriptionResponseDto.serializer(), jsonString)

        assertEquals(response, deserialized)
    }

    @Test
    fun createTranscriptionResponseVerboseDto_serialization() {
        val word = TranscriptionWordDto(word = "hello", start = 0.0f, end = 1.0f)
        val segment = TranscriptionSegmentDto(
            id = 0,
            seek = 0,
            start = 0.0f,
            end = 1.0f,
            text = "hello",
            tokens = listOf(1, 2, 3),
            temperature = 0.0f,
            avgLogprob = -0.5f,
            compressionRatio = 1.0f,
            noSpeechProb = 0.1f
        )
        
        val response = CreateTranscriptionResponseVerboseDto(
            language = "en",
            duration = 5.0f,
            text = "Hello world",
            words = listOf(word),
            segments = listOf(segment)
        )

        val jsonString = json.encodeToString(CreateTranscriptionResponseVerboseDto.serializer(), response)
        val deserialized = json.decodeFromString(CreateTranscriptionResponseVerboseDto.serializer(), jsonString)

        assertEquals(response, deserialized)
    }

    @Test
    fun multipartTranscriptionRequest_equality() {
        val audioData = byteArrayOf(1, 2, 3, 4)
        val request = CreateTranscriptionRequestDto(model = "whisper-1")
        
        val multipart1 = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = "test.wav",
            contentType = "audio/wav",
            request = request
        )
        
        val multipart2 = MultipartTranscriptionRequest(
            audioData = audioData,
            fileName = "test.wav",
            contentType = "audio/wav",
            request = request
        )

        assertEquals(multipart1, multipart2)
        assertEquals(multipart1.hashCode(), multipart2.hashCode())
    }

    @Test
    fun multipartTranscriptionRequest_inequality() {
        val audioData1 = byteArrayOf(1, 2, 3, 4)
        val audioData2 = byteArrayOf(5, 6, 7, 8)
        val request = CreateTranscriptionRequestDto(model = "whisper-1")
        
        val multipart1 = MultipartTranscriptionRequest(
            audioData = audioData1,
            fileName = "test.wav",
            contentType = "audio/wav",
            request = request
        )
        
        val multipart2 = MultipartTranscriptionRequest(
            audioData = audioData2,
            fileName = "test.wav",
            contentType = "audio/wav",
            request = request
        )

        assertNotEquals(multipart1, multipart2)
    }
}