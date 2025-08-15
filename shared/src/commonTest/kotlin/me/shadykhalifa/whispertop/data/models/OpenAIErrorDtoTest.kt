package me.shadykhalifa.whispertop.data.models

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OpenAIErrorDtoTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun openAIErrorDto_serialization() {
        val error = OpenAIErrorDto(
            message = "Invalid API key",
            type = "authentication_error",
            param = null,
            code = "invalid_api_key"
        )

        val jsonString = json.encodeToString(OpenAIErrorDto.serializer(), error)
        val deserialized = json.decodeFromString(OpenAIErrorDto.serializer(), jsonString)

        assertEquals(error, deserialized)
    }

    @Test
    fun openAIErrorResponseDto_serialization() {
        val error = OpenAIErrorDto(
            message = "Rate limit exceeded",
            type = "rate_limit_error",
            param = "requests",
            code = "rate_limit_exceeded"
        )
        val errorResponse = OpenAIErrorResponseDto(error = error)

        val jsonString = json.encodeToString(OpenAIErrorResponseDto.serializer(), errorResponse)
        val deserialized = json.decodeFromString(OpenAIErrorResponseDto.serializer(), jsonString)

        assertEquals(errorResponse, deserialized)
    }

    @Test
    fun openAIErrorDto_withNullValues() {
        val error = OpenAIErrorDto(
            message = "Server error",
            type = "server_error"
        )

        assertEquals("Server error", error.message)
        assertEquals("server_error", error.type)
        assertEquals(null, error.param)
        assertEquals(null, error.code)
    }

    @Test
    fun openAIException_authenticationException() {
        val exception = OpenAIException.AuthenticationException("Invalid API key")
        
        assertEquals("Invalid API key", exception.message)
        assertEquals(401, exception.statusCode)
    }

    @Test
    fun openAIException_rateLimitException() {
        val exception = OpenAIException.RateLimitException("Rate limit exceeded")
        
        assertEquals("Rate limit exceeded", exception.message)
        assertEquals(429, exception.statusCode)
    }

    @Test
    fun openAIException_serverException() {
        val exception = OpenAIException.ServerException("Internal server error")
        
        assertEquals("Internal server error", exception.message)
        assertEquals(500, exception.statusCode)
    }

    @Test
    fun openAIException_invalidRequestException() {
        val exception = OpenAIException.InvalidRequestException("Invalid request format")
        
        assertEquals("Invalid request format", exception.message)
        assertEquals(400, exception.statusCode)
    }

    @Test
    fun openAIException_networkException() {
        val cause = RuntimeException("Connection timeout")
        val exception = OpenAIException.NetworkException("Network error", cause)
        
        assertEquals("Network error", exception.message)
        assertEquals(cause, exception.cause)
        assertEquals(0, exception.statusCode)
    }

    @Test
    fun openAIException_networkExceptionWithoutCause() {
        val exception = OpenAIException.NetworkException("Network error")
        
        assertEquals("Network error", exception.message)
        assertEquals(null, exception.cause)
        assertEquals(0, exception.statusCode)
    }

    @Test
    fun openAIException_unknownException() {
        val exception = OpenAIException.UnknownException("Unknown error")
        
        assertEquals("Unknown error", exception.message)
        assertEquals(0, exception.statusCode)
    }
}