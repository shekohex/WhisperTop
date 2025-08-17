package me.shadykhalifa.whispertop.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.ui.overlay.components.AnimationConstants
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.domain.models.PerformanceWarning
import me.shadykhalifa.whispertop.domain.models.WarningType
import me.shadykhalifa.whispertop.domain.models.WarningSeverity
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

/**
 * Enhanced performance monitoring utilities for animations, UI components, and system metrics
 */
object PerformanceMonitor : KoinComponent {
    
    private var frameCount = 0L
    private var lastFpsCheck = System.currentTimeMillis()
    private var currentFps = 0f
    
    private var lastRenderTime = 0L
    
    /**
     * Frame rate limiter for smooth animations
     * 
     * @param minFrameTimeMs Minimum time between frames in milliseconds
     * @return true if enough time has passed to render next frame
     */
    fun shouldRenderFrame(minFrameTimeMs: Long = AnimationConstants.MIN_FRAME_TIME_MS): Boolean {
        val currentTime = System.currentTimeMillis()
        
        return if (currentTime - lastRenderTime >= minFrameTimeMs) {
            lastRenderTime = currentTime
            true
        } else {
            false
        }
    }
    
    /**
     * Track frame rate for performance monitoring
     */
    fun trackFrame() {
        frameCount++
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastFpsCheck
        
        if (timeDiff >= 1000) { // Calculate FPS every second
            currentFps = (frameCount * 1000f) / timeDiff
            frameCount = 0
            lastFpsCheck = currentTime
            
            // Log performance warning if FPS is too low
            if (currentFps < 30f) {
                android.util.Log.w("PerformanceMonitor", "Low FPS detected: $currentFps")
            }
        }
    }
    
    /**
     * Get current FPS reading
     */
    fun getCurrentFps(): Float = currentFps
    
    /**
     * Composable that monitors and throttles recomposition rate
     * 
     * @param key Value to monitor for changes
     * @param throttleMs Minimum time between updates in milliseconds
     * @param onUpdate Callback when throttled update should occur
     */
    @Composable
    fun <T> ThrottledUpdate(
        key: T,
        throttleMs: Long = 100L,
        onUpdate: (T) -> Unit
    ) {
        var lastUpdateTime by remember { mutableLongStateOf(0L) }
        var pendingValue by remember { mutableLongStateOf(0L) }
        
        LaunchedEffect(key) {
            val currentTime = System.currentTimeMillis()
            pendingValue = currentTime
            
            if (currentTime - lastUpdateTime >= throttleMs) {
                onUpdate(key)
                lastUpdateTime = currentTime
            } else {
                // Delay until throttle period expires
                delay(throttleMs - (currentTime - lastUpdateTime))
                if (pendingValue == currentTime) { // Only update if no newer value pending
                    onUpdate(key)
                    lastUpdateTime = System.currentTimeMillis()
                }
            }
        }
    }
    
    /**
     * Enhanced memory usage monitoring with metrics collection
     */
    object MemoryMonitor {
        
        suspend fun logMemoryUsage(
            sessionId: String? = null,
            tag: String = "PerformanceMonitor",
            context: String = ""
        ) {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val usagePercent = (usedMemory * 100) / maxMemory
            
            android.util.Log.d(tag, "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB ($usagePercent%)")
            
            // Record memory snapshot if session ID is provided
            sessionId?.let { id ->
                try {
                    val metricsCollector: MetricsCollector = get()
                    metricsCollector.recordMemorySnapshot(id, context)
                } catch (e: Exception) {
                    android.util.Log.w(tag, "Failed to record memory snapshot: ${e.message}")
                }
            }
            
            if (usagePercent > 80) {
                android.util.Log.w(tag, "High memory usage detected: $usagePercent%")
                
                // Record performance warning if session ID is provided
                sessionId?.let { id ->
                    try {
                        val metricsCollector: MetricsCollector = get()
                        val warning = PerformanceWarning(
                            timestamp = System.currentTimeMillis(),
                            type = WarningType.HIGH_MEMORY_USAGE,
                            message = "High memory usage detected: $usagePercent%",
                            severity = if (usagePercent > 90) WarningSeverity.CRITICAL else WarningSeverity.WARNING,
                            context = context,
                            metrics = mapOf(
                                "usagePercent" to usagePercent.toString(),
                                "usedMemoryMB" to (usedMemory / 1024 / 1024).toString(),
                                "maxMemoryMB" to (maxMemory / 1024 / 1024).toString()
                            )
                        )
                        metricsCollector.recordPerformanceWarning(id, warning)
                    } catch (e: Exception) {
                        android.util.Log.w(tag, "Failed to record performance warning: ${e.message}")
                    }
                }
            }
        }
        
        fun isMemoryLow(): Boolean {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val usagePercent = (usedMemory * 100) / maxMemory
            return usagePercent > 85
        }
        
        fun getCurrentMemoryInfo(): MemoryInfo {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val freeMemory = runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val usagePercent = (usedMemory * 100) / maxMemory
            
            return MemoryInfo(
                usedMemory = usedMemory,
                freeMemory = freeMemory,
                maxMemory = maxMemory,
                usagePercent = usagePercent.toFloat()
            )
        }
    }
    
    /**
     * Performance timing utilities
     */
    object TimingMonitor {
        
        inline fun <T> measureTime(
            operation: String,
            sessionId: String? = null,
            block: () -> T
        ): T {
            val startTime = System.currentTimeMillis()
            val result = block()
            val duration = System.currentTimeMillis() - startTime
            
            android.util.Log.d("TimingMonitor", "$operation took ${duration}ms")
            
            // Log performance warning for slow operations
            if (duration > 5000) { // 5 seconds
                android.util.Log.w("TimingMonitor", "Slow operation detected: $operation took ${duration}ms")
                
                sessionId?.let { id ->
                    try {
                        val metricsCollector: MetricsCollector = get()
                        val warning = PerformanceWarning(
                            timestamp = System.currentTimeMillis(),
                            type = WarningType.SLOW_TRANSCRIPTION, // Generic for any slow operation
                            message = "Slow operation: $operation took ${duration}ms",
                            severity = WarningSeverity.WARNING,
                            context = operation,
                            metrics = mapOf("duration" to duration.toString())
                        )
                        // Note: This is async but we can't suspend here, so we fire and forget
                        kotlinx.coroutines.GlobalScope.launch {
                            metricsCollector.recordPerformanceWarning(id, warning)
                        }
                    } catch (e: Exception) {
                        android.util.Log.w("TimingMonitor", "Failed to record timing warning: ${e.message}")
                    }
                }
            }
            
            return result
        }
        
        suspend inline fun <T> measureTimeAsync(
            operation: String,
            sessionId: String? = null,
            crossinline block: suspend () -> T
        ): T {
            val startTime = System.currentTimeMillis()
            val result = block()
            val duration = System.currentTimeMillis() - startTime
            
            android.util.Log.d("TimingMonitor", "$operation took ${duration}ms")
            
            // Log performance warning for slow operations
            if (duration > 5000) { // 5 seconds
                android.util.Log.w("TimingMonitor", "Slow operation detected: $operation took ${duration}ms")
                
                sessionId?.let { id ->
                    try {
                        val metricsCollector: MetricsCollector = get()
                        val warning = PerformanceWarning(
                            timestamp = System.currentTimeMillis(),
                            type = WarningType.SLOW_TRANSCRIPTION,
                            message = "Slow operation: $operation took ${duration}ms",
                            severity = WarningSeverity.WARNING,
                            context = operation,
                            metrics = mapOf("duration" to duration.toString())
                        )
                        metricsCollector.recordPerformanceWarning(id, warning)
                    } catch (e: Exception) {
                        android.util.Log.w("TimingMonitor", "Failed to record timing warning: ${e.message}")
                    }
                }
            }
            
            return result
        }
    }
    
    data class MemoryInfo(
        val usedMemory: Long,
        val freeMemory: Long,
        val maxMemory: Long,
        val usagePercent: Float
    )
}