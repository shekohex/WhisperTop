package me.shadykhalifa.whispertop.utils

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock

class CircuitBreaker(
    private val failureThreshold: Int = 5,
    private val recoveryTimeoutMs: Long = 60000, // 1 minute
    private val successThreshold: Int = 2
) {
    
    private var failureCount = 0
    private var lastFailureTime: Long = 0
    private var successCount = 0
    private var state = CircuitBreakerState.CLOSED
    private val mutex = Mutex()
    
    enum class CircuitBreakerState {
        CLOSED,    // Normal operation
        OPEN,      // Failing, rejecting calls
        HALF_OPEN  // Testing if service recovered
    }
    
    suspend fun <T> execute(operation: suspend () -> T): T {
        mutex.withLock {
            when (state) {
                CircuitBreakerState.OPEN -> {
                    if (shouldAttemptReset()) {
                        state = CircuitBreakerState.HALF_OPEN
                        successCount = 0
                    } else {
                        throw CircuitBreakerOpenException("Circuit breaker is open")
                    }
                }
                CircuitBreakerState.HALF_OPEN -> {
                    // Allow limited calls to test recovery
                }
                CircuitBreakerState.CLOSED -> {
                    // Normal operation
                }
            }
        }
        
        return try {
            val result = operation()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw e
        }
    }
    
    private fun shouldAttemptReset(): Boolean {
        return Clock.System.now().toEpochMilliseconds() - lastFailureTime >= recoveryTimeoutMs
    }
    
    private suspend fun onSuccess() {
        mutex.withLock {
            when (state) {
                CircuitBreakerState.HALF_OPEN -> {
                    successCount++
                    if (successCount >= successThreshold) {
                        state = CircuitBreakerState.CLOSED
                        failureCount = 0
                    }
                }
                CircuitBreakerState.CLOSED -> {
                    failureCount = 0
                }
                CircuitBreakerState.OPEN -> {
                    // Should not happen, but reset if it does
                    state = CircuitBreakerState.CLOSED
                    failureCount = 0
                }
            }
        }
    }
    
    private suspend fun onFailure() {
        mutex.withLock {
            failureCount++
            lastFailureTime = Clock.System.now().toEpochMilliseconds()
            
            when (state) {
                CircuitBreakerState.CLOSED -> {
                    if (failureCount >= failureThreshold) {
                        state = CircuitBreakerState.OPEN
                    }
                }
                CircuitBreakerState.HALF_OPEN -> {
                    state = CircuitBreakerState.OPEN
                }
                CircuitBreakerState.OPEN -> {
                    // Already open, just update failure time
                }
            }
        }
    }
    
    suspend fun getCurrentState(): CircuitBreakerState {
        return mutex.withLock { state }
    }
    
    suspend fun getFailureCount(): Int {
        return mutex.withLock { failureCount }
    }
    
    suspend fun reset() {
        mutex.withLock {
            state = CircuitBreakerState.CLOSED
            failureCount = 0
            successCount = 0
        }
    }
}

class CircuitBreakerOpenException(message: String) : Exception(message)