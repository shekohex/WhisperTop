package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RetryServiceTest {

    private val retryService = RetryServiceImpl()

    @Test
    fun `withRetry should succeed on first attempt`() = runTest {
        var attempts = 0
        
        val result = retryService.withRetry {
            attempts++
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(1, attempts)
    }

    @Test
    fun `withRetry should retry on failure and eventually succeed`() = runTest {
        var attempts = 0
        
        val result = retryService.withRetry(maxRetries = 3) {
            attempts++
            if (attempts < 3) {
                throw RuntimeException("Temporary failure")
            }
            "success"
        }
        
        assertEquals("success", result)
        assertEquals(3, attempts)
    }

    @Test
    fun `withRetry should fail after max retries`() = runTest {
        var attempts = 0
        
        assertFailsWith<RuntimeException> {
            retryService.withRetry(maxRetries = 2) {
                attempts++
                throw RuntimeException("Persistent failure")
            }
        }
        
        assertEquals(3, attempts) // maxRetries + 1 (initial attempt)
    }

    @Test
    fun `withRetry should respect retry predicate`() = runTest {
        var attempts = 0
        val networkError = RuntimeException("network error")
        val authError = RuntimeException("authentication failed")
        
        // Should retry network errors but not auth errors
        assertFailsWith<RuntimeException> {
            retryService.withRetry(
                maxRetries = 3,
                retryPredicate = { it.message?.contains("network") == true }
            ) {
                attempts++
                if (attempts == 1) {
                    throw networkError // This should be retried
                } else {
                    throw authError // This should not be retried
                }
            }
        }
        
        assertEquals(2, attempts)
    }

    @Test
    fun `withRetry should use exponential backoff`() = runTest {
        var attempts = 0
        
        assertFailsWith<RuntimeException> {
            retryService.withRetry(
                maxRetries = 2,
                initialDelay = 1L, // Very small delay for testing
                exponentialBackoff = true
            ) {
                attempts++
                throw RuntimeException("Always fails")
            }
        }
        
        assertEquals(3, attempts) // maxRetries + 1 (initial attempt)
    }

    @Test
    fun `withRetry should respect max delay`() = runTest {
        var attempts = 0
        val startTime = System.currentTimeMillis()
        
        assertFailsWith<RuntimeException> {
            retryService.withRetry(
                maxRetries = 3,
                initialDelay = 1000L,
                maxDelay = 500L, // Smaller than initial delay
                exponentialBackoff = true
            ) {
                attempts++
                throw RuntimeException("Always fails")
            }
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        assertEquals(4, attempts)
        // Should not exceed maxDelay * number of retries
        assertTrue(totalTime < 2000L, "Total time was $totalTime ms, expected less than 2000ms")
    }

    @Test
    fun `RetryPredicates networkErrors should identify network errors correctly`() {
        assertTrue(RetryPredicates.networkErrors(RuntimeException("network timeout")))
        assertTrue(RetryPredicates.networkErrors(RuntimeException("connection failed")))
        assertTrue(RetryPredicates.networkErrors(RuntimeException("TIMEOUT occurred")))
        assertTrue(RetryPredicates.networkErrors(java.net.SocketTimeoutException()))
        assertTrue(RetryPredicates.networkErrors(java.net.UnknownHostException()))
        assertTrue(RetryPredicates.networkErrors(java.io.IOException()))
        
        assertEquals(false, RetryPredicates.networkErrors(RuntimeException("authentication failed")))
        assertEquals(false, RetryPredicates.networkErrors(IllegalArgumentException("invalid input")))
    }

    @Test
    fun `RetryPredicates transientAudioErrors should identify audio errors correctly`() {
        assertTrue(RetryPredicates.transientAudioErrors(RuntimeException("device unavailable")))
        assertTrue(RetryPredicates.transientAudioErrors(RuntimeException("audio focus lost")))
        assertTrue(RetryPredicates.transientAudioErrors(RuntimeException("temporarily blocked")))
        
        assertEquals(false, RetryPredicates.transientAudioErrors(RuntimeException("permission denied")))
        assertEquals(false, RetryPredicates.transientAudioErrors(RuntimeException("invalid configuration")))
    }

    @Test
    fun `RetryPredicates apiRateLimits should identify rate limit errors correctly`() {
        assertTrue(RetryPredicates.apiRateLimits(RuntimeException("rate limit exceeded")))
        assertTrue(RetryPredicates.apiRateLimits(RuntimeException("HTTP 429 error")))
        assertTrue(RetryPredicates.apiRateLimits(RuntimeException("too many requests")))
        
        assertEquals(false, RetryPredicates.apiRateLimits(RuntimeException("invalid api key")))
        assertEquals(false, RetryPredicates.apiRateLimits(RuntimeException("server error")))
    }

    @Test
    fun `RetryPredicates temporaryServerErrors should identify server errors correctly`() {
        assertTrue(RetryPredicates.temporaryServerErrors(RuntimeException("HTTP 503 service unavailable")))
        assertTrue(RetryPredicates.temporaryServerErrors(RuntimeException("502 bad gateway")))
        assertTrue(RetryPredicates.temporaryServerErrors(RuntimeException("server error occurred")))
        assertTrue(RetryPredicates.temporaryServerErrors(RuntimeException("service unavailable")))
        
        assertEquals(false, RetryPredicates.temporaryServerErrors(RuntimeException("400 bad request")))
        assertEquals(false, RetryPredicates.temporaryServerErrors(RuntimeException("authentication failed")))
    }

    @Test
    fun `RetryPredicates nonRetryableErrors should exclude authentication errors`() {
        assertEquals(false, RetryPredicates.nonRetryableErrors(RuntimeException("authentication failed")))
        assertEquals(false, RetryPredicates.nonRetryableErrors(RuntimeException("unauthorized access")))
        assertEquals(false, RetryPredicates.nonRetryableErrors(RuntimeException("invalid api key")))
        assertEquals(false, RetryPredicates.nonRetryableErrors(RuntimeException("permission denied")))
        
        assertTrue(RetryPredicates.nonRetryableErrors(RuntimeException("network timeout")))
        assertTrue(RetryPredicates.nonRetryableErrors(RuntimeException("rate limit exceeded")))
    }
}