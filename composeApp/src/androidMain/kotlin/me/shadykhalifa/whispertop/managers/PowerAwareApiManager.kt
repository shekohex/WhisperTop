package me.shadykhalifa.whispertop.managers

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages API requests with power-aware batching and scheduling
 */
class PowerAwareApiManager(
    private val powerManager: PowerManagementUtil,
    private val scope: CoroutineScope
) {
    
    private val pendingRequests = ConcurrentLinkedQueue<ApiRequest>()
    private val isProcessing = AtomicBoolean(false)
    private var processingJob: Job? = null
    
    data class ApiRequest(
        val id: String,
        val priority: Priority,
        val maxDelay: Long,
        val createdAt: Long = System.currentTimeMillis(),
        val execute: suspend () -> Unit
    )
    
    enum class Priority {
        IMMEDIATE, // Execute immediately regardless of power state
        HIGH,      // Execute with minimal delay
        NORMAL,    // Can be delayed for power optimization
        LOW        // Can be significantly delayed or batched
    }
    
    /**
     * Submit an API request for power-aware execution
     */
    fun submitRequest(
        id: String,
        priority: Priority,
        maxDelay: Long = 30_000L, // 30 seconds default max delay
        execute: suspend () -> Unit
    ) {
        val request = ApiRequest(id, priority, maxDelay, execute = execute)
        pendingRequests.offer(request)
        
        if (priority == Priority.IMMEDIATE) {
            // Execute immediately
            scope.launch {
                try {
                    execute()
                } catch (e: Exception) {
                    // Handle error
                }
            }
        } else {
            scheduleProcessing()
        }
    }
    
    /**
     * Schedule request processing based on power state
     */
    private fun scheduleProcessing() {
        if (isProcessing.compareAndSet(false, true)) {
            processingJob = scope.launch {
                try {
                    processRequests()
                } finally {
                    isProcessing.set(false)
                }
            }
        }
    }
    
    /**
     * Process pending requests based on power optimization
     */
    private suspend fun processRequests() {
        while (pendingRequests.isNotEmpty()) {
            val powerState = powerManager.powerState.value
            
            if (shouldDelayProcessing(powerState)) {
                val delay = powerManager.getRecommendedOperationDelay()
                if (delay > 0) {
                    delay(delay)
                    continue
                }
            }
            
            // Process batch of requests
            val batchSize = getBatchSize(powerState)
            val batch = mutableListOf<ApiRequest>()
            
            repeat(batchSize) {
                val request = pendingRequests.poll() ?: return@repeat
                
                // Check if request has expired
                val age = System.currentTimeMillis() - request.createdAt
                if (age <= request.maxDelay) {
                    batch.add(request)
                }
            }
            
            if (batch.isNotEmpty()) {
                processBatch(batch, powerState)
                
                // Add delay between batches if needed
                val batchDelay = getBatchDelay(powerState)
                if (batchDelay > 0) {
                    delay(batchDelay)
                }
            }
        }
    }
    
    /**
     * Process a batch of API requests
     */
    private suspend fun processBatch(
        batch: List<ApiRequest>,
        powerState: PowerManagementUtil.PowerState
    ) {
        // Sort by priority
        val sortedBatch = batch.sortedBy { request ->
            when (request.priority) {
                Priority.IMMEDIATE -> 0
                Priority.HIGH -> 1
                Priority.NORMAL -> 2
                Priority.LOW -> 3
            }
        }
        
        if (powerState.isInDozeMode || powerState.isInPowerSaveMode) {
            // Execute requests sequentially in power save mode
            for (request in sortedBatch) {
                try {
                    request.execute()
                    delay(500) // Small delay between requests
                } catch (e: Exception) {
                    // Handle error
                }
            }
        } else {
            // Execute requests concurrently when power is available
            val jobs = sortedBatch.map { request ->
                scope.async {
                    try {
                        request.execute()
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
            jobs.awaitAll()
        }
    }
    
    /**
     * Determine if processing should be delayed
     */
    private fun shouldDelayProcessing(powerState: PowerManagementUtil.PowerState): Boolean {
        return powerState.isInDozeMode || 
               (powerState.isInPowerSaveMode && !powerState.isIgnoringBatteryOptimizations)
    }
    
    /**
     * Get optimal batch size based on power state
     */
    private fun getBatchSize(powerState: PowerManagementUtil.PowerState): Int {
        return when {
            powerState.isInDozeMode -> 1 // Process one at a time in doze mode
            powerState.isInPowerSaveMode -> 2 // Small batches in power save mode
            !powerState.isIgnoringBatteryOptimizations -> 3 // Medium batches if not optimized
            else -> 5 // Larger batches when fully optimized
        }
    }
    
    /**
     * Get delay between batches
     */
    private fun getBatchDelay(powerState: PowerManagementUtil.PowerState): Long {
        return when {
            powerState.isInDozeMode -> 10_000L // 10 seconds between batches
            powerState.isInPowerSaveMode -> 5_000L // 5 seconds
            !powerState.isIgnoringBatteryOptimizations -> 2_000L // 2 seconds
            else -> 500L // Minimal delay when optimized
        }
    }
    
    /**
     * Cancel all pending requests
     */
    fun cancelAllRequests() {
        pendingRequests.clear()
        processingJob?.cancel()
        isProcessing.set(false)
    }
    
    /**
     * Get number of pending requests
     */
    fun getPendingRequestCount(): Int {
        return pendingRequests.size
    }
    
    /**
     * Check if manager is currently processing requests
     */
    fun isProcessingRequests(): Boolean {
        return isProcessing.get()
    }
}

/**
 * Extension function to create a power-aware API manager
 */
fun PowerManagementUtil.createApiManager(scope: CoroutineScope): PowerAwareApiManager {
    return PowerAwareApiManager(this, scope)
}