package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.models.*
import kotlin.test.*

class ErrorHandlingInterceptorTest {

    @Test
    fun `should throw AuthenticationException for 401 status`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": {"message": "Invalid API key", "type": "invalid_request_error"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.AuthenticationException> {
            response.validateOrThrow()
        }

        assertEquals(401, exception.statusCode)
        assertTrue(exception.message.contains("Authentication failed"))
    }

    @Test
    fun `should throw RateLimitException for 429 status`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": {"message": "Rate limit exceeded", "type": "rate_limit_error"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.RateLimitException> {
            response.validateOrThrow()
        }

        assertEquals(429, exception.statusCode)
        assertTrue(exception.message.contains("Rate limit exceeded"))
    }

    @Test
    fun `should throw ServerException for 500 status`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": {"message": "Internal server error", "type": "server_error"}}""",
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.ServerException> {
            response.validateOrThrow()
        }

        assertEquals(500, exception.statusCode)
        assertTrue(exception.message.contains("Server error"))
    }

    @Test
    fun `should throw InvalidRequestException for 400 status`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"error": {"message": "Bad request", "type": "invalid_request_error"}}""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.InvalidRequestException> {
            response.validateOrThrow()
        }

        assertEquals(400, exception.statusCode)
        assertTrue(exception.message.contains("Bad request"))
    }

    @Test
    fun `should handle plain text error responses`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Service temporarily unavailable",
                status = HttpStatusCode.ServiceUnavailable,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.ServerException> {
            response.validateOrThrow()
        }

        assertEquals(503, exception.statusCode)
        assertTrue(exception.message.contains("Server error"))
    }

    @Test
    fun `should handle malformed JSON error responses`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """Invalid JSON response""",
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        // Don't install ContentNegotiation for this test to test raw response handling
        val client = HttpClient(mockEngine)

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.InvalidRequestException> {
            response.validateOrThrow()
        }

        assertEquals(400, exception.statusCode)
        assertTrue(exception.message.contains("Bad request"))
    }

    @Test
    fun `should map unknown status codes to UnknownException`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = "Unknown error",
                status = HttpStatusCode(600, "Unknown"),
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        val exception = assertFailsWith<OpenAIException.UnknownException> {
            response.validateOrThrow()
        }

        assertEquals(600, exception.statusCode)
        assertTrue(exception.message.contains("Unknown error"))
    }

    @Test
    fun `HttpStatusCode extension should map correctly`() {
        assertTrue(HttpStatusCode.Unauthorized.toOpenAIException() is OpenAIException.AuthenticationException)
        assertTrue(HttpStatusCode.TooManyRequests.toOpenAIException() is OpenAIException.RateLimitException)
        assertTrue(HttpStatusCode.InternalServerError.toOpenAIException() is OpenAIException.ServerException)
        assertTrue(HttpStatusCode.BadRequest.toOpenAIException() is OpenAIException.InvalidRequestException)
        assertTrue(HttpStatusCode(418, "I'm a teapot").toOpenAIException() is OpenAIException.InvalidRequestException)
        assertTrue(HttpStatusCode(600, "Unknown").toOpenAIException() is OpenAIException.UnknownException)
    }

    @Test
    fun `should not throw on success responses`() = runTest {
        val mockEngine = MockEngine { request ->
            respond(
                content = """{"success": true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }

        val response = client.get("https://api.openai.com/v1/test")
        // Should not throw any exception
        response.validateOrThrow()
    }
}