package me.shadykhalifa.whispertop.managers

import kotlinx.coroutines.*
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Task scheduler that adapts to device power state for optimal performance
 */
class PowerAwareTaskScheduler(
    private val powerManager: PowerManagementUtil,
    private val scope: CoroutineScope
) {
    
    private val taskQueue = PriorityBlockingQueue<ScheduledTask>()
    private val isRunning = AtomicBoolean(false)
    private var schedulerJob: Job? = null
    
    data class ScheduledTask(
        val id: String,
        val priority: TaskPriority,
        val delay: Long,
        val maxDelay: Long,
        val canBeBatched: Boolean,
        val powerRequirement: PowerRequirement,
        val createdAt: Long = System.currentTimeMillis(),
        val execute: suspend () -> Unit
    ) : Comparable<ScheduledTask> {
        
        override fun compareTo(other: ScheduledTask): Int {
            // First compare by priority
            val priorityComparison = priority.ordinal.compareTo(other.priority.ordinal)
            if (priorityComparison != 0) return priorityComparison
            
            // Then by creation time (FIFO for same priority)
            return createdAt.compareTo(other.createdAt)
        }
    }
    
    enum class TaskPriority {
        CRITICAL,   // Must execute regardless of power state
        HIGH,       // Execute with minimal delay
        NORMAL,     // Can be delayed for power optimization
        LOW,        // Can be significantly delayed
        BACKGROUND  // Execute only when power conditions are optimal
    }
    
    enum class PowerRequirement {
        ANY,        // Can execute in any power state
        OPTIMAL,    // Prefer optimal power conditions
        FULL_POWER  // Only execute when not in power save mode
    }
    
    /**
     * Start the task scheduler
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            schedulerJob = scope.launch {
                runScheduler()
            }
        }
    }
    
    /**
     * Stop the task scheduler
     */
    fun stop() {
        isRunning.set(false)
        schedulerJob?.cancel()
        taskQueue.clear()
    }
    
    /**
     * Schedule a task for execution
     */
    fun schedule(
        id: String,
        priority: TaskPriority,
        delay: Long = 0L,
        maxDelay: Long = 300_000L, // 5 minutes default
        canBeBatched: Boolean = true,
        powerRequirement: PowerRequirement = PowerRequirement.ANY,
        execute: suspend () -> Unit
    ) {
        val task = ScheduledTask(
            id = id,
            priority = priority,
            delay = delay,
            maxDelay = maxDelay,
            canBeBatched = canBeBatched,
            powerRequirement = powerRequirement,
            execute = execute
        )
        
        taskQueue.offer(task)
    }
    
    /**
     * Main scheduler loop
     */
    private suspend fun runScheduler() {
        while (isRunning.get()) {
            try {
                val powerState = powerManager.powerState.value
                
                if (shouldPauseScheduling(powerState)) {
                    delay(powerManager.getRecommendedOperationDelay())
                    continue
                }
                
                val batch = collectBatch(powerState)
                if (batch.isNotEmpty()) {
                    executeBatch(batch, powerState)
                }
                
                // Small delay between scheduling cycles
                delay(1000)
                
            } catch (e: Exception) {
                // Handle scheduling errors
                delay(5000) // Wait before retrying
            }
        }
    }
    
    /**
     * Check if scheduling should be paused
     */
    private fun shouldPauseScheduling(powerState: PowerManagementUtil.PowerState): Boolean {
        return powerState.isInDozeMode && taskQueue.isEmpty()
    }
    
    /**
     * Collect a batch of tasks ready for execution
     */
    private fun collectBatch(powerState: PowerManagementUtil.PowerState): List<ScheduledTask> {
        val batch = mutableListOf<ScheduledTask>()
        val maxBatchSize = getBatchSize(powerState)
        val currentTime = System.currentTimeMillis()
        
        while (batch.size < maxBatchSize) {
            val task = taskQueue.peek() ?: break
            
            // Check if task is ready or expired
            val taskAge = currentTime - task.createdAt
            val isReady = taskAge >= task.delay
            val isExpired = taskAge >= task.maxDelay
            val meetsRequirements = meetsPowerRequirements(task, powerState)
            
            if ((isReady && meetsRequirements) || isExpired) {
                taskQueue.poll()?.let { batch.add(it) }
            } else {
                break // Wait for next cycle
            }
        }
        
        return batch
    }
    
    /**
     * Check if task meets power requirements
     */
    private fun meetsPowerRequirements(
        task: ScheduledTask,
        powerState: PowerManagementUtil.PowerState
    ): Boolean {
        return when (task.powerRequirement) {
            PowerRequirement.ANY -> true
            PowerRequirement.OPTIMAL -> powerState.isIgnoringBatteryOptimizations
            PowerRequirement.FULL_POWER -> !powerState.isInPowerSaveMode && !powerState.isInDozeMode
        }
    }
    
    /**
     * Execute a batch of tasks
     */
    private suspend fun executeBatch(
        batch: List<ScheduledTask>,
        powerState: PowerManagementUtil.PowerState
    ) {
        // Sort by priority
        val sortedBatch = batch.sortedBy { it.priority.ordinal }
        
        if (powerState.isInDozeMode || powerState.isInPowerSaveMode) {
            // Execute sequentially in power save mode
            for (task in sortedBatch) {
                executeTask(task)
                delay(500) // Small delay between tasks
            }
        } else {
            // Group batchable tasks
            val (batchable, individual) = sortedBatch.partition { it.canBeBatched }
            
            // Execute individual tasks first
            for (task in individual) {
                executeTask(task)
            }
            
            // Execute batchable tasks concurrently
            if (batchable.isNotEmpty()) {
                val jobs = batchable.map { task ->
                    scope.async { executeTask(task) }
                }
                jobs.awaitAll()
            }
        }
    }
    
    /**
     * Execute a single task
     */
    private suspend fun executeTask(task: ScheduledTask) {
        try {
            task.execute()
        } catch (e: Exception) {
            // Handle task execution error
        }
    }
    
    /**
     * Get optimal batch size based on power state
     */
    private fun getBatchSize(powerState: PowerManagementUtil.PowerState): Int {
        return when {
            powerState.isInDozeMode -> 1
            powerState.isInPowerSaveMode -> 3
            !powerState.isIgnoringBatteryOptimizations -> 5
            else -> 10
        }
    }
    
    /**
     * Get number of pending tasks
     */
    fun getPendingTaskCount(): Int {
        return taskQueue.size
    }
    
    /**
     * Check if scheduler is running
     */
    fun isRunning(): Boolean {
        return isRunning.get()
    }
}