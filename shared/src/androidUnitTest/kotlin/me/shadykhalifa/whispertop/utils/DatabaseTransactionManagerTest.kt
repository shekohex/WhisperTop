package me.shadykhalifa.whispertop.utils

import androidx.room.withTransaction
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Comprehensive test suite for DatabaseTransactionManager
 * Tests transaction handling, circuit breaker integration, and error scenarios
 */
class DatabaseTransactionManagerTest {
    
    private lateinit var mockDatabase: AppDatabase
    private lateinit var mockCircuitBreaker: CircuitBreaker
    private lateinit var transactionManager: DatabaseTransactionManager
    
    @Before
    fun setup() {
        mockDatabase = mockk()
        mockCircuitBreaker = spyk(CircuitBreaker(failureThreshold = 2, recoveryTimeoutMs = 1000))
        transactionManager = DatabaseTransactionManager(mockDatabase, mockCircuitBreaker)
        
        // Mock the Room withTransaction extension
        mockkStatic("androidx.room.RoomDatabaseKt")
    }
    
    @After
    fun teardown() {
        // Clean up any resources
    }
    
    @Test
    fun `withTransaction should execute successfully and return result`() = runTest {
        // Given
        val expectedResult = "success"
        coEvery { mockDatabase.withTransaction(any<suspend () -> String>()) } coAnswers {
            val lambda = firstArg<suspend () -> String>()
            lambda()
        }
        
        // When
        val result = transactionManager.withTransaction("test_operation") {
            expectedResult
        }
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedResult, (result as Result.Success).data)
        coVerify { mockDatabase.withTransaction(any<suspend () -> String>()) }
    }
    
    @Test
    fun `withTransaction should handle database exceptions`() = runTest {
        // Given
        val exception = RuntimeException("Database error")
        coEvery { mockDatabase.withTransaction(any<suspend () -> String>()) } throws exception
        
        // When
        val result = transactionManager.withTransaction("test_operation") {
            "should not reach here"
        }
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DatabaseException)
        assertTrue(result.exception.message!!.contains("Transaction failed: test_operation"))
    }
    
    @Test
    fun `withTransaction should handle circuit breaker open state`() = runTest {
        // Given
        coEvery { mockCircuitBreaker.execute<String>(any()) } throws CircuitBreakerOpenException("Circuit breaker is open")
        
        // When
        val result = transactionManager.withTransaction("test_operation") {
            "should not reach here"
        }
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DatabaseException)
        assertTrue(result.exception.message!!.contains("Database circuit breaker is open"))
    }
    
    @Test
    fun `withBatchTransaction should process all batches successfully`() = runTest {
        // Given
        val items = (1..100).map { "item_$it" }
        val batchSize = 10
        var processedItems = 0
        
        coEvery { mockDatabase.withTransaction(any<suspend () -> Int>()) } coAnswers {
            val lambda = firstArg<suspend () -> Int>()
            lambda()
        }
        
        // When
        val result = transactionManager.withBatchTransaction(
            operationName = "batch_test",
            batchSize = batchSize,
            items = items
        ) { batch ->
            processedItems += batch.size
        }
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(100, (result as Result.Success).data)
        assertEquals(100, processedItems)
        
        // Verify transactions were called for each batch
        val expectedBatches = (items.size + batchSize - 1) / batchSize
        coVerify(exactly = expectedBatches) { mockDatabase.withTransaction(any<suspend () -> Int>()) }
    }
    
    @Test
    fun `withBatchTransaction should fail fast on batch error`() = runTest {
        // Given
        val items = (1..50).map { "item_$it" }
        val exception = RuntimeException("Batch processing error")
        
        coEvery { mockDatabase.withTransaction(any<suspend () -> Int>()) } throws exception
        
        // When
        val result = transactionManager.withBatchTransaction(
            operationName = "batch_test",
            batchSize = 10,
            items = items
        ) { _ ->
            // Should not complete
        }
        
        // Then
        assertTrue(result is Result.Error)
        assertTrue((result as Result.Error).exception is DatabaseException)
        assertTrue(result.exception.message!!.contains("Batch operation failed at batch 0"))
    }
    
    @Test
    fun `withConditionalTransaction should execute operation when condition is true`() = runTest {
        // Given
        val expectedResult = "conditional_success"
        coEvery { mockDatabase.withTransaction(any<suspend () -> String?>()) } coAnswers {
            val lambda = firstArg<suspend () -> String?>()
            lambda()
        }
        
        // When
        val result = transactionManager.withConditionalTransaction(
            operationName = "conditional_test",
            condition = { true }
        ) {
            expectedResult
        }
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedResult, (result as Result.Success).data)
    }
    
    @Test
    fun `withConditionalTransaction should return null when condition is false`() = runTest {
        // Given
        coEvery { mockDatabase.withTransaction(any<suspend () -> String?>()) } coAnswers {
            val lambda = firstArg<suspend () -> String?>()
            lambda()
        }
        
        // When
        val result = transactionManager.withConditionalTransaction(
            operationName = "conditional_test",
            condition = { false }
        ) {
            "should not execute"
        }
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(null, (result as Result.Success).data)
    }
    
    @Test
    fun `withReadOnlyTransaction should execute without full transaction overhead`() = runTest {
        // Given
        val expectedResult = "read_only_result"
        
        // When
        val result = transactionManager.withReadOnlyTransaction("read_test") {
            expectedResult
        }
        
        // Then
        assertTrue(result is Result.Success)
        assertEquals(expectedResult, (result as Result.Success).data)
        
        // Verify that withTransaction was NOT called (read-only doesn't need full transaction)
        coVerify(exactly = 0) { mockDatabase.withTransaction(any<suspend () -> Any>()) }
    }
    
    @Test
    fun `getCircuitBreakerStatus should return current state`() = runTest {
        // Given
        val expectedState = CircuitBreaker.CircuitBreakerState.CLOSED
        coEvery { mockCircuitBreaker.getCurrentState() } returns expectedState
        
        // When
        val result = transactionManager.getCircuitBreakerStatus()
        
        // Then
        assertEquals(expectedState, result)
    }
    
    @Test
    fun `resetCircuitBreaker should reset the circuit breaker`() = runTest {
        // When
        transactionManager.resetCircuitBreaker()
        
        // Then
        coVerify { mockCircuitBreaker.reset() }
    }
}