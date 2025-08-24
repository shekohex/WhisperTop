package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BackgroundThreadManagerTest {
    
    @Test
    fun `executeTask should run task on appropriate dispatcher`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        
        val result = manager.executeTask(ThreadType.COMPUTATION, "test-task") {
            "computation-result"
        }
        
        assertEquals("computation-result", result)
    }
    
    @Test
    fun `launchTask should execute task asynchronously`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        var executed = false
        
        val job = manager.launchTask(ThreadType.DATABASE_IO, "async-task") {
            delay(100)
            executed = true
        }
        
        // Give the task a chance to start
        yield()
        
        // Task should be running
        assertEquals(1, manager.getActiveTaskCount())
        
        // Wait for completion
        job.join()
        
        assertTrue(executed)
        assertEquals(0, manager.getActiveTaskCount())
    }
    
    @Test
    fun `getActiveTaskCountByType should track tasks by type`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        
        val job1 = manager.launchTask(ThreadType.DATABASE_IO, "db-task") {
            delay(Long.MAX_VALUE) // Never-ending task for testing
        }
        
        val job2 = manager.launchTask(ThreadType.COMPUTATION, "comp-task") {
            delay(Long.MAX_VALUE) // Never-ending task for testing
        }
        
        // Give the tasks a chance to start
        yield()
        
        assertEquals(2, manager.getActiveTaskCount())
        assertEquals(1, manager.getActiveTaskCountByType(ThreadType.DATABASE_IO))
        assertEquals(1, manager.getActiveTaskCountByType(ThreadType.COMPUTATION))
        assertEquals(0, manager.getActiveTaskCountByType(ThreadType.NETWORK_IO))
        
        job1.cancel()
        job2.cancel()
    }
    
    @Test
    fun `cancelAllTasks should cancel all running tasks`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        
        val job1 = manager.launchTask(ThreadType.DATABASE_IO, "task1") {
            delay(1000)
        }
        
        val job2 = manager.launchTask(ThreadType.COMPUTATION, "task2") {
            delay(1000)
        }
        
        assertEquals(2, manager.getActiveTaskCount())
        
        manager.cancelAllTasks()
        
        // Wait a bit for cancellation to take effect
        delay(100)
        
        assertEquals(0, manager.getActiveTaskCount())
        assertTrue(job1.isCancelled)
        assertTrue(job2.isCancelled)
    }
    
    @Test
    fun `cancelTasksByType should cancel tasks of specific type only`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        
        val dbJob = manager.launchTask(ThreadType.DATABASE_IO, "db-task") {
            delay(1000)
        }
        
        val compJob = manager.launchTask(ThreadType.COMPUTATION, "comp-task") {
            delay(1000)
        }
        
        assertEquals(2, manager.getActiveTaskCount())
        
        manager.cancelTasksByType(ThreadType.DATABASE_IO)
        
        // Wait for cancellation
        delay(100)
        
        assertTrue(dbJob.isCancelled)
        assertTrue(compJob.isActive)
        
        compJob.cancel()
    }
    
    @Test
    fun `getDispatcherForType should return correct dispatchers`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        
        val dbDispatcher = manager.getDispatcherForType(ThreadType.DATABASE_IO)
        val networkDispatcher = manager.getDispatcherForType(ThreadType.NETWORK_IO)
        val computationDispatcher = manager.getDispatcherForType(ThreadType.COMPUTATION)
        
        // All IO operations should use IO dispatcher
        assertEquals(dbDispatcher, networkDispatcher)
        // Computation should use Default dispatcher
        assertTrue(computationDispatcher != dbDispatcher)
    }
    
    @Test
    fun `extension functions should work correctly`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        
        val dbResult = manager.executeDatabase("db-operation") {
            "database-result"
        }
        
        val compResult = manager.executeComputation("comp-operation") {
            42
        }
        
        assertEquals("database-result", dbResult)
        assertEquals(42, compResult)
    }
    
    @Test
    fun `task metrics should be tracked correctly`() = runTest {
        val manager = BackgroundThreadManagerImpl()
        val metricsFlow = manager.observeTaskMetrics()
        
        var receivedMetrics: List<TaskMetrics>? = null
        val job = launch {
            metricsFlow.collect { metrics ->
                receivedMetrics = metrics
            }
        }
        
        // Execute a task
        manager.executeTask(ThreadType.COMPUTATION, "metrics-test") {
            delay(50)
            "result"
        }
        
        // Give time for metrics to be collected
        delay(100)
        
        // Check if metrics were recorded
        assertTrue(receivedMetrics?.isNotEmpty() == true)
        val metrics = receivedMetrics?.first()
        assertEquals("metrics-test", metrics?.taskId)
        assertEquals(ThreadType.COMPUTATION, metrics?.threadType)
        assertEquals(TaskStatus.COMPLETED, metrics?.status)
        assertTrue((metrics?.duration ?: 0) >= 50)
        
        job.cancel()
    }
}