package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

enum class ThreadType {
    DATABASE_IO,      // For database operations
    NETWORK_IO,       // For network requests
    FILE_IO,          // For file operations
    COMPUTATION,      // For CPU-intensive calculations
    AUDIO_PROCESSING, // For audio processing tasks
    CACHE_OPERATIONS  // For cache management
}

data class TaskMetrics(
    val taskId: String,
    val threadType: ThreadType,
    val startTime: Long,
    val endTime: Long? = null,
    val status: TaskStatus
) {
    val duration: Long?
        get() = endTime?.let { it - startTime }
}

enum class TaskStatus {
    RUNNING,
    COMPLETED,
    FAILED,
    CANCELLED
}

interface BackgroundThreadManager {
    suspend fun <T> executeTask(
        threadType: ThreadType,
        taskId: String = "task_${kotlin.random.Random.nextInt(10000)}",
        block: suspend CoroutineScope.() -> T
    ): T
    
    fun launchTask(
        threadType: ThreadType,
        taskId: String = "task_${kotlin.random.Random.nextInt(10000)}",
        block: suspend CoroutineScope.() -> Unit
    ): Job
    
    fun cancelAllTasks()
    fun cancelTasksByType(threadType: ThreadType)
    fun getActiveTaskCount(): Int
    fun getActiveTaskCountByType(threadType: ThreadType): Int
    fun observeTaskMetrics(): Flow<List<TaskMetrics>>
    fun getDispatcherForType(threadType: ThreadType): CoroutineDispatcher
}

class BackgroundThreadManagerImpl : BackgroundThreadManager {
    
    private val supervisorJob = SupervisorJob()
    
    // Custom scopes for different thread types
    private val databaseScope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val networkScope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val fileScope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private val computationScope = CoroutineScope(Dispatchers.Default + supervisorJob)
    private val audioProcessingScope = CoroutineScope(Dispatchers.Default + supervisorJob)
    private val cacheScope = CoroutineScope(Dispatchers.Default + supervisorJob)
    
    // Task tracking
    private val activeTasks = mutableMapOf<String, Job>()
    private val taskMetrics = mutableMapOf<String, TaskMetrics>()
    private val _taskMetricsFlow = MutableStateFlow<List<TaskMetrics>>(emptyList())
    
    override suspend fun <T> executeTask(
        threadType: ThreadType,
        taskId: String,
        block: suspend CoroutineScope.() -> T
    ): T {
        val dispatcher = getDispatcherForType(threadType)
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        // Track task start
        trackTaskStart(taskId, threadType, startTime)
        
        return try {
            withContext(dispatcher) {
                val result = block()
                trackTaskCompletion(taskId, TaskStatus.COMPLETED)
                result
            }
        } catch (e: Exception) {
            trackTaskCompletion(taskId, TaskStatus.FAILED)
            throw e
        }
    }
    
    override fun launchTask(
        threadType: ThreadType,
        taskId: String,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        val scope = getScopeForType(threadType)
        val startTime = Clock.System.now().toEpochMilliseconds()
        
        val job = scope.launch {
            try {
                trackTaskStart(taskId, threadType, startTime)
                block()
                trackTaskCompletion(taskId, TaskStatus.COMPLETED)
            } catch (e: Exception) {
                trackTaskCompletion(taskId, TaskStatus.FAILED)
                throw e
            }
        }
        
        // Track the job
        activeTasks[taskId] = job
        
        // Clean up when job completes
        job.invokeOnCompletion {
            activeTasks.remove(taskId)
            if (it != null) {
                trackTaskCompletion(taskId, TaskStatus.CANCELLED)
            }
        }
        
        return job
    }
    
    override fun cancelAllTasks() {
        activeTasks.values.forEach { it.cancel() }
        activeTasks.clear()
        supervisorJob.cancelChildren()
    }
    
    override fun cancelTasksByType(threadType: ThreadType) {
        val tasksToCancel = taskMetrics.filter { (_, metrics) ->
            metrics.threadType == threadType && metrics.status == TaskStatus.RUNNING
        }
        
        tasksToCancel.forEach { (taskId, _) ->
            activeTasks[taskId]?.cancel()
            activeTasks.remove(taskId)
        }
    }
    
    override fun getActiveTaskCount(): Int = activeTasks.size
    
    override fun getActiveTaskCountByType(threadType: ThreadType): Int {
        return taskMetrics.values.count { 
            it.threadType == threadType && it.status == TaskStatus.RUNNING 
        }
    }
    
    override fun observeTaskMetrics(): Flow<List<TaskMetrics>> = _taskMetricsFlow.asStateFlow()
    
    override fun getDispatcherForType(threadType: ThreadType): CoroutineDispatcher {
        return when (threadType) {
            ThreadType.DATABASE_IO -> Dispatchers.IO
            ThreadType.NETWORK_IO -> Dispatchers.IO
            ThreadType.FILE_IO -> Dispatchers.IO
            ThreadType.COMPUTATION -> Dispatchers.Default
            ThreadType.AUDIO_PROCESSING -> Dispatchers.Default
            ThreadType.CACHE_OPERATIONS -> Dispatchers.Default
        }
    }
    
    private fun getScopeForType(threadType: ThreadType): CoroutineScope {
        return when (threadType) {
            ThreadType.DATABASE_IO -> databaseScope
            ThreadType.NETWORK_IO -> networkScope
            ThreadType.FILE_IO -> fileScope
            ThreadType.COMPUTATION -> computationScope
            ThreadType.AUDIO_PROCESSING -> audioProcessingScope
            ThreadType.CACHE_OPERATIONS -> cacheScope
        }
    }
    
    private fun trackTaskStart(taskId: String, threadType: ThreadType, startTime: Long) {
        val metrics = TaskMetrics(
            taskId = taskId,
            threadType = threadType,
            startTime = startTime,
            status = TaskStatus.RUNNING
        )
        taskMetrics[taskId] = metrics
        updateTaskMetricsFlow()
    }
    
    private fun trackTaskCompletion(taskId: String, status: TaskStatus) {
        val existingMetrics = taskMetrics[taskId]
        if (existingMetrics != null) {
            val updatedMetrics = existingMetrics.copy(
                endTime = Clock.System.now().toEpochMilliseconds(),
                status = status
            )
            taskMetrics[taskId] = updatedMetrics
            updateTaskMetricsFlow()
        }
    }
    
    private fun updateTaskMetricsFlow() {
        _taskMetricsFlow.value = taskMetrics.values.toList()
    }
}

// Extension functions for easier usage
suspend inline fun <T> BackgroundThreadManager.executeDatabase(
    taskId: String = "db_${kotlin.random.Random.nextInt(10000)}",
    noinline block: suspend CoroutineScope.() -> T
): T = executeTask(ThreadType.DATABASE_IO, taskId, block)

suspend inline fun <T> BackgroundThreadManager.executeNetwork(
    taskId: String = "net_${kotlin.random.Random.nextInt(10000)}",
    noinline block: suspend CoroutineScope.() -> T
): T = executeTask(ThreadType.NETWORK_IO, taskId, block)

suspend inline fun <T> BackgroundThreadManager.executeComputation(
    taskId: String = "comp_${kotlin.random.Random.nextInt(10000)}",
    noinline block: suspend CoroutineScope.() -> T
): T = executeTask(ThreadType.COMPUTATION, taskId, block)

fun BackgroundThreadManager.launchDatabase(
    taskId: String = "db_${kotlin.random.Random.nextInt(10000)}",
    block: suspend CoroutineScope.() -> Unit
): Job = launchTask(ThreadType.DATABASE_IO, taskId, block)

fun BackgroundThreadManager.launchComputation(
    taskId: String = "comp_${kotlin.random.Random.nextInt(10000)}",
    block: suspend CoroutineScope.() -> Unit
): Job = launchTask(ThreadType.COMPUTATION, taskId, block)