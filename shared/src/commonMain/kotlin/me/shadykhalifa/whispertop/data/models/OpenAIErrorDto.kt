package me.shadykhalifa.whispertop.data.models

import io.ktor.http.*
import kotlinx.serialization.Serializable

@Serializable
data class OpenAIErrorResponseDto(
    val error: OpenAIErrorDto
)

@Serializable
data class OpenAIErrorDto(
    val message: String,
    val type: String,
    val param: String? = null,
    val code: String? = null
)

sealed class OpenAIException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    abstract val statusCode: Int
    
    data class AuthenticationException(override val message: String, override val statusCode: Int = 401) : OpenAIException(message)
    data class RateLimitException(override val message: String, override val statusCode: Int = 429) : OpenAIException(message)
    data class ServerException(override val message: String, override val statusCode: Int = 500) : OpenAIException(message)
    data class InvalidRequestException(override val message: String, override val statusCode: Int = 400) : OpenAIException(message)
    data class NetworkException(override val message: String, override val cause: Throwable? = null, override val statusCode: Int = 0) : OpenAIException(message, cause)
    data class UnknownException(override val message: String, override val statusCode: Int = 0) : OpenAIException(message)
}

fun HttpStatusCode.toOpenAIException(errorMessage: String? = null): OpenAIException {
    val message = errorMessage ?: "HTTP error: $value $description"
    return when (value) {
        401 -> OpenAIException.AuthenticationException("Authentication failed: $message", value)
        429 -> OpenAIException.RateLimitException("Rate limit exceeded: $message", value)
        in 500..599 -> OpenAIException.ServerException("Server error: $message", value)
        in 400..499 -> OpenAIException.InvalidRequestException("Bad request: $message", value)
        else -> OpenAIException.UnknownException("Unknown error: $message", value)
    }
}