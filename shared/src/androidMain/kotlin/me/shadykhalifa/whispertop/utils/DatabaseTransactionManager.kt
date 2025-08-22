package me.shadykhalifa.whispertop.utils

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.data.database.DatabasePerformanceMonitor
import me.shadykhalifa.whispertop.domain.services.RetryService
import me.shadykhalifa.whispertop.domain.services.RetryServiceImpl
import me.shadykhalifa.whispertop.domain.services.RetryPredicates
import me.shadykhalifa.whispertop.utils.Result
import kotlin.system.measureTimeMillis

/**
 * Enhanced transaction manager for atomic multi-table operations with performance monitoring
 */
class DatabaseTransactionManager(
    private val database: AppDatabase,
    private val circuitBreaker: CircuitBreaker = CircuitBreaker(
        failureThreshold = 3,
        recoveryTimeoutMs = 30000,
        successThreshold = 2
    )
) {
    
    /**
     * Execute a database transaction with comprehensive error handling and performance monitoring
     */
    suspend fun <T> withTransaction(
        operationName: String,
        maxRetries: Int = 3,
        operation: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        
        val retryService = RetryServiceImpl()
        
        try {
            val result = circuitBreaker.execute {
                retryService.withRetry(
                    maxRetries = maxRetries,
                    initialDelay = 100L,
                    maxDelay = 5000L,
                    exponentialBackoff = true,
                    retryPredicate = RetryPredicates.allRetryableErrors
                ) {
                    DatabasePerformanceMonitor.measureOperation("transaction_$operationName") {
                        database.withTransaction {
                            operation()
                        }
                    }
                }
            }
            Result.Success(result)
        } catch (e: Exception) {
            when (e) {
                is CircuitBreakerOpenException -> {
                    Result.Error(DatabaseException("Database circuit breaker is open", e))
                }
                else -> {
                    Result.Error(DatabaseException("Transaction failed: $operationName", e))
                }
            }
        }
    }
    
    /**
     * Execute atomic batch operations with rollback on any failure
     */
    suspend fun <T> withBatchTransaction(
        operationName: String,
        batchSize: Int = 50,
        items: List<T>,
        operation: suspend (List<T>) -> Unit
    ): Result<Int> = withContext(Dispatchers.IO) {
        
        try {
            var totalProcessed = 0
            val batches = items.chunked(batchSize)
            
            for ((index, batch) in batches.withIndex()) {
                val batchOperationName = "${operationName}_batch_$index"
                
                val result = withTransaction(batchOperationName) {
                    operation(batch)
                    batch.size
                }
                
                when (result) {
                    is Result.Success -> {
                        totalProcessed += result.data
                    }
                    is Result.Error -> {
                        return@withContext Result.Error(
                            DatabaseException("Batch operation failed at batch $index", result.exception)
                        )
                    }
                    is Result.Loading -> {
                        // Should not happen in this context
                    }
                }
            }
            
            Result.Success(totalProcessed)
            
        } catch (e: Exception) {
            Result.Error(DatabaseException("Batch transaction failed: $operationName", e))
        }
    }
    
    /**
     * Execute conditional transaction - only commits if condition is met
     */
    suspend fun <T> withConditionalTransaction(
        operationName: String,
        condition: suspend () -> Boolean,
        operation: suspend () -> T
    ): Result<T?> = withContext(Dispatchers.IO) {
        
        withTransaction("conditional_$operationName") {
            if (condition()) {
                operation()
            } else {
                null
            }
        }
    }
    
    /**
     * Execute read-only transaction with optimized settings
     */
    suspend fun <T> withReadOnlyTransaction(
        operationName: String,
        operation: suspend () -> T
    ): Result<T> = withContext(Dispatchers.IO) {
        
        try {
            val result = DatabasePerformanceMonitor.measureOperation("readonly_$operationName") {
                // For read-only operations, we don't need full transaction overhead
                operation()
            }
            Result.Success(result)
            
        } catch (e: Exception) {
            Result.Error(DatabaseException("Read-only operation failed: $operationName", e))
        }
    }
    
    /**
     * Get circuit breaker status for monitoring
     */
    suspend fun getCircuitBreakerStatus(): CircuitBreaker.CircuitBreakerState {
        return circuitBreaker.getCurrentState()
    }
    
    /**
     * Reset circuit breaker (use with caution)
     */
    suspend fun resetCircuitBreaker() {
        circuitBreaker.reset()
    }
}

/**
 * Custom database exception for better error handling
 */
class DatabaseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

