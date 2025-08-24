package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.shadykhalifa.whispertop.domain.models.PerformanceMetrics

data class OperationTiming(
    val operationName: String,
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long = endTime - startTime,
    val metadata: Map<String, Any> = emptyMap()
)

data class FrameMetrics(
    val frameTimeMs: Long,
    val isJanky: Boolean,
    val timestamp: Long = System.currentTimeMillis()
) {
    val fps: Double
        get() = if (frameTimeMs > 0) 1000.0 / frameTimeMs else 0.0
}

interface PerformanceMonitor {
    fun startOperation(operationName: String, metadata: Map<String, Any> = emptyMap()): String
    fun endOperation(operationId: String)
    fun recordFrameTime(frameTimeMs: Long)
    fun recordCustomMetric(name: String, value: Double, unit: String = "")
    fun getAverageFrameTime(windowSizeMs: Long = 1000L): Double
    fun getOperationStats(operationName: String): OperationStats?
    fun observePerformanceMetrics(): Flow<PerformanceMetrics>
    fun observeFrameMetrics(): Flow<FrameMetrics>
    fun clearMetrics()
}

data class OperationStats(
    val operationName: String,
    val count: Int,
    val averageDurationMs: Double,
    val minDurationMs: Long,
    val maxDurationMs: Long,
    val totalDurationMs: Long
)

class PerformanceMonitorImpl : PerformanceMonitor {
    
    private val activeOperations = mutableMapOf<String, Pair<String, Long>>() // operationId to (name, startTime)
    private val completedOperations = mutableListOf<OperationTiming>()
    private val frameMetrics = mutableListOf<FrameMetrics>()
    private val customMetrics = mutableMapOf<String, MutableList<Pair<Double, Long>>>()
    
    private val _performanceMetrics = MutableStateFlow(createPerformanceMetrics())
    private val _frameMetrics = MutableSharedFlow<FrameMetrics>()
    
    override fun startOperation(operationName: String, metadata: Map<String, Any>): String {
        val operationId = generateOperationId(operationName)
        val startTime = System.nanoTime()
        
        activeOperations[operationId] = operationName to startTime
        return operationId
    }
    
    override fun endOperation(operationId: String) {
        val endTime = System.nanoTime()
        val operation = activeOperations.remove(operationId)
        
        if (operation != null) {
            val (operationName, startTime) = operation
            val timing = OperationTiming(
                operationName = operationName,
                startTime = startTime,
                endTime = endTime,
                durationMs = (endTime - startTime) / 1_000_000L // Convert to milliseconds
            )
            
            completedOperations.add(timing)
            
            // Keep only recent operations (last 1000)
            if (completedOperations.size > 1000) {
                completedOperations.removeAt(0)
            }
            
            updatePerformanceMetrics()
        }
    }
    
    override fun recordFrameTime(frameTimeMs: Long) {
        val isJanky = frameTimeMs > 16 // 60 FPS threshold
        val frameMetric = FrameMetrics(frameTimeMs, isJanky)
        
        frameMetrics.add(frameMetric)
        
        // Keep only recent frame metrics (last 1000 frames)
        if (frameMetrics.size > 1000) {
            frameMetrics.removeAt(0)
        }
        
        // Emit frame metric
        _frameMetrics.tryEmit(frameMetric)
        updatePerformanceMetrics()
    }
    
    override fun recordCustomMetric(name: String, value: Double, unit: String) {
        val metrics = customMetrics.getOrPut(name) { mutableListOf() }
        metrics.add(value to System.currentTimeMillis())
        
        // Keep only recent metrics (last 1000)
        if (metrics.size > 1000) {
            metrics.removeAt(0)
        }
        
        updatePerformanceMetrics()
    }
    
    override fun getAverageFrameTime(windowSizeMs: Long): Double {
        val cutoffTime = System.currentTimeMillis() - windowSizeMs
        val recentFrames = frameMetrics.filter { it.timestamp >= cutoffTime }
        
        return if (recentFrames.isNotEmpty()) {
            recentFrames.map { it.frameTimeMs }.average()
        } else {
            0.0
        }
    }
    
    override fun getOperationStats(operationName: String): OperationStats? {
        val operations = completedOperations.filter { it.operationName == operationName }
        
        if (operations.isEmpty()) return null
        
        val durations = operations.map { it.durationMs }
        
        return OperationStats(
            operationName = operationName,
            count = operations.size,
            averageDurationMs = durations.average(),
            minDurationMs = durations.minOrNull() ?: 0L,
            maxDurationMs = durations.maxOrNull() ?: 0L,
            totalDurationMs = durations.sum()
        )
    }
    
    override fun observePerformanceMetrics(): Flow<PerformanceMetrics> = _performanceMetrics.asStateFlow()
    
    override fun observeFrameMetrics(): Flow<FrameMetrics> = _frameMetrics
    
    override fun clearMetrics() {
        completedOperations.clear()
        frameMetrics.clear()
        customMetrics.clear()
        activeOperations.clear()
        updatePerformanceMetrics()
    }
    
    private fun createPerformanceMetrics(): PerformanceMetrics {
        val recentFrames = frameMetrics.takeLast(100)
        val averageFrameTime = if (recentFrames.isNotEmpty()) {
            recentFrames.map { it.frameTimeMs }.average()
        } else 0.0
        
        val jankyFramePercentage = if (recentFrames.isNotEmpty()) {
            (recentFrames.count { it.isJanky }.toDouble() / recentFrames.size) * 100
        } else 0.0
        
        return PerformanceMetrics(
            averageFrameTime = averageFrameTime,
            jankyFramePercentage = jankyFramePercentage,
            activeOperations = activeOperations.size,
            completedOperations = completedOperations.size,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun updatePerformanceMetrics() {
        _performanceMetrics.value = createPerformanceMetrics()
    }
    
    private fun generateOperationId(operationName: String): String {
        return "${operationName}_${System.currentTimeMillis()}_${(0..999).random()}"
    }
}

// Extension functions for easier usage
inline fun <T> PerformanceMonitor.measureOperation(
    operationName: String,
    metadata: Map<String, Any> = emptyMap(),
    block: () -> T
): T {
    val operationId = startOperation(operationName, metadata)
    try {
        return block()
    } finally {
        endOperation(operationId)
    }
}

suspend inline fun <T> PerformanceMonitor.measureSuspendOperation(
    operationName: String,
    metadata: Map<String, Any> = emptyMap(),
    crossinline block: suspend () -> T
): T {
    val operationId = startOperation(operationName, metadata)
    try {
        return block()
    } finally {
        endOperation(operationId)
    }
}