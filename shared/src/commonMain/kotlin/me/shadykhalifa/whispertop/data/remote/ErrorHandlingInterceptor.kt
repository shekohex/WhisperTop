package me.shadykhalifa.whispertop.data.remote

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.SerializationException
import me.shadykhalifa.whispertop.data.models.*

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val exception: OpenAIException) : ApiResult<Nothing>()
}

suspend fun handleErrorResponse(response: HttpResponse): OpenAIException {
    val statusCode = response.status
    
    val errorMessage = try {
        // First try to get plain text response
        val responseText = response.bodyAsText()
        
        // If the content type is JSON and the text looks like JSON, try to parse it
        if (response.contentType()?.match(ContentType.Application.Json) == true && 
            responseText.trim().startsWith("{") && responseText.trim().endsWith("}")) {
            try {
                // Try to manually parse the JSON to extract error message
                // This is a simple approach that doesn't require ContentNegotiation
                if (responseText.contains("\"error\"") && responseText.contains("\"message\"")) {
                    // Extract message using regex for simple cases
                    val messageRegex = """"message"\s*:\s*"([^"]+)"""".toRegex()
                    val messageMatch = messageRegex.find(responseText)
                    val typeRegex = """"type"\s*:\s*"([^"]+)"""".toRegex()
                    val typeMatch = typeRegex.find(responseText)
                    
                    if (messageMatch != null) {
                        val message = messageMatch.groupValues[1]
                        val type = typeMatch?.groupValues?.get(1) ?: "unknown"
                        "$message ($type)"
                    } else {
                        responseText.takeIf { it.isNotBlank() } ?: statusCode.description
                    }
                } else {
                    responseText.takeIf { it.isNotBlank() } ?: statusCode.description
                }
            } catch (e: Exception) {
                responseText.takeIf { it.isNotBlank() } ?: statusCode.description
            }
        } else {
            responseText.takeIf { it.isNotBlank() } ?: statusCode.description
        }
    } catch (e: Exception) {
        statusCode.description
    }
    
    return when (statusCode.value) {
        401 -> OpenAIException.AuthenticationException(
            message = "Authentication failed: $errorMessage",
            statusCode = statusCode.value
        )
        429 -> OpenAIException.RateLimitException(
            message = "Rate limit exceeded: $errorMessage",
            statusCode = statusCode.value
        )
        in 400..499 -> OpenAIException.InvalidRequestException(
            message = "Bad request: $errorMessage",
            statusCode = statusCode.value
        )
        in 500..599 -> OpenAIException.ServerException(
            message = "Server error: $errorMessage",
            statusCode = statusCode.value
        )
        else -> OpenAIException.UnknownException(
            message = "Unknown error: $errorMessage",
            statusCode = statusCode.value
        )
    }
}

suspend fun <T> HttpResponse.asApiResult(transform: suspend (HttpResponse) -> T): ApiResult<T> {
    return when {
        status.isSuccess() -> {
            try {
                ApiResult.Success(transform(this))
            } catch (e: Exception) {
                ApiResult.Error(
                    OpenAIException.NetworkException(
                        message = "Failed to parse response: ${e.message}",
                        cause = e
                    )
                )
            }
        }
        else -> ApiResult.Error(handleErrorResponse(this))
    }
}

suspend fun HttpResponse.validateOrThrow() {
    if (!status.isSuccess()) {
        throw handleErrorResponse(this)
    }
}

fun HttpClientConfig<*>.installErrorHandling() {
}