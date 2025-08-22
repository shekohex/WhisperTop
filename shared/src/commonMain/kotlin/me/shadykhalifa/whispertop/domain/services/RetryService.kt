package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import me.shadykhalifa.whispertop.domain.models.NetworkException
import me.shadykhalifa.whispertop.domain.models.TimeoutException
import kotlin.math.min
import kotlin.math.pow

interface RetryService {
    suspend fun <T> withRetry(
        maxRetries: Int = 3,
        initialDelay: Long = 1000L,
        maxDelay: Long = 10000L,
        exponentialBackoff: Boolean = true,
        retryPredicate: (Throwable) -> Boolean = { true },
        operation: suspend () -> T
    ): T
}

class RetryServiceImpl : RetryService {
    
    override suspend fun <T> withRetry(
        maxRetries: Int,
        initialDelay: Long,
        maxDelay: Long,
        exponentialBackoff: Boolean,
        retryPredicate: (Throwable) -> Boolean,
        operation: suspend () -> T
    ): T {
        var lastException: Throwable? = null
        
        repeat(maxRetries + 1) { attempt ->
            try {
                return operation()
            } catch (e: Throwable) {
                lastException = e
                
                if (attempt == maxRetries || !retryPredicate(e)) {
                    throw e
                }
                
                val delayTime = if (exponentialBackoff) {
                    min(initialDelay * 2.0.pow(attempt).toLong(), maxDelay)
                } else {
                    initialDelay
                }
                
                delay(delayTime)
            }
        }
        
        throw lastException ?: RuntimeException("Retry operation failed")
    }
}

object RetryPredicates {
    
    val networkErrors: (Throwable) -> Boolean = { throwable ->
        when {
            throwable.message?.contains("network", ignoreCase = true) == true -> true
            throwable.message?.contains("timeout", ignoreCase = true) == true -> true
            throwable.message?.contains("connection", ignoreCase = true) == true -> true
            throwable is CancellationException -> true
            throwable is NetworkException -> true
            throwable::class.simpleName?.contains("IOException", ignoreCase = true) == true -> true
            throwable::class.simpleName?.contains("SocketTimeoutException", ignoreCase = true) == true -> true
            throwable::class.simpleName?.contains("UnknownHostException", ignoreCase = true) == true -> true
            throwable::class.simpleName?.contains("ConnectException", ignoreCase = true) == true -> true
            throwable is Exception -> throwable.message?.contains("IOException", ignoreCase = true) == true
            else -> false
        }
    }
    
    val transientAudioErrors: (Throwable) -> Boolean = { throwable ->
        when {
            throwable.message?.contains("device unavailable", ignoreCase = true) == true -> true
            throwable.message?.contains("audio focus", ignoreCase = true) == true -> true
            throwable.message?.contains("temporarily", ignoreCase = true) == true -> true
            else -> false
        }
    }
    
    val apiRateLimits: (Throwable) -> Boolean = { throwable ->
        when {
            throwable.message?.contains("rate limit", ignoreCase = true) == true -> true
            throwable.message?.contains("429", ignoreCase = true) == true -> true
            throwable.message?.contains("too many requests", ignoreCase = true) == true -> true
            else -> false
        }
    }
    
    val temporaryServerErrors: (Throwable) -> Boolean = { throwable ->
        when {
            throwable.message?.contains("503", ignoreCase = true) == true -> true
            throwable.message?.contains("502", ignoreCase = true) == true -> true
            throwable.message?.contains("server error", ignoreCase = true) == true -> true
            throwable.message?.contains("service unavailable", ignoreCase = true) == true -> true
            else -> false
        }
    }
    
    val allRetryableErrors: (Throwable) -> Boolean = { throwable ->
        networkErrors(throwable) || 
        transientAudioErrors(throwable) || 
        apiRateLimits(throwable) || 
        temporaryServerErrors(throwable)
    }
    
    val nonRetryableErrors: (Throwable) -> Boolean = { throwable ->
        when {
            throwable.message?.contains("authentication", ignoreCase = true) == true -> false
            throwable.message?.contains("unauthorized", ignoreCase = true) == true -> false
            throwable.message?.contains("forbidden", ignoreCase = true) == true -> false
            throwable.message?.contains("invalid api key", ignoreCase = true) == true -> false
            throwable.message?.contains("permission denied", ignoreCase = true) == true -> false
            else -> allRetryableErrors(throwable)
        }
    }
}