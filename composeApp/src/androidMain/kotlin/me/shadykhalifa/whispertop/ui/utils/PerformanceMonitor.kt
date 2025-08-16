package me.shadykhalifa.whispertop.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import me.shadykhalifa.whispertop.ui.overlay.components.AnimationConstants

/**
 * Performance monitoring utilities for animations and UI components
 */
object PerformanceMonitor {
    
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
     * Memory usage monitoring
     */
    object MemoryMonitor {
        
        fun logMemoryUsage(tag: String = "PerformanceMonitor") {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val usagePercent = (usedMemory * 100) / maxMemory
            
            android.util.Log.d(tag, "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB ($usagePercent%)")
            
            if (usagePercent > 80) {
                android.util.Log.w(tag, "High memory usage detected: $usagePercent%")
            }
        }
        
        fun isMemoryLow(): Boolean {
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val usagePercent = (usedMemory * 100) / maxMemory
            return usagePercent > 85
        }
    }
}