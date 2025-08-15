package me.shadykhalifa.whispertop.data.models

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
    data class AuthenticationException(override val message: String) : OpenAIException(message)
    data class RateLimitException(override val message: String) : OpenAIException(message)
    data class ServerException(override val message: String) : OpenAIException(message)
    data class InvalidRequestException(override val message: String) : OpenAIException(message)
    data class NetworkException(override val message: String, override val cause: Throwable? = null) : OpenAIException(message, cause)
    data class UnknownException(override val message: String) : OpenAIException(message)
}