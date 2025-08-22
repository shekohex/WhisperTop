package me.shadykhalifa.whispertop.benchmark

/**
 * Performance baselines for WhisperTop benchmark tests.
 * These baselines help detect performance regressions.
 */
object PerformanceBaselines {
    
    // Audio Processing Baselines (in milliseconds)
    const val SMALL_BUFFER_PROCESSING_MS = 5      // 1KB buffer
    const val MEDIUM_BUFFER_PROCESSING_MS = 20    // 16KB buffer  
    const val LARGE_BUFFER_PROCESSING_MS = 100    // 64KB buffer
    
    // File I/O Baselines (in milliseconds)
    const val SMALL_WAV_CREATION_MS = 10          // ~0.5 seconds audio
    const val MEDIUM_WAV_CREATION_MS = 50         // ~5 seconds audio
    const val LARGE_WAV_CREATION_MS = 200         // ~30 seconds audio
    
    // Database Operation Baselines (in milliseconds)
    const val SINGLE_INSERT_MS = 5               // Single record insert
    const val SMALL_BATCH_INSERT_MS = 20         // 10 records
    const val MEDIUM_BATCH_INSERT_MS = 100       // 100 records
    const val LARGE_BATCH_INSERT_MS = 500        // 1000 records
    const val QUERY_BY_ID_MS = 2                 // Single record query
    const val SEARCH_QUERY_MS = 50               // Text search query
    const val COMPLEX_QUERY_MS = 100             // Complex aggregation query
    
    // UI Rendering Baselines (in milliseconds)
    const val SIMPLE_COMPOSE_RENDER_MS = 16      // 60 FPS target
    const val COMPLEX_LIST_RENDER_MS = 32        // Heavy list rendering
    const val NAVIGATION_TRANSITION_MS = 100     // Screen transitions
    
    // Memory Usage Baselines (in KB)
    const val SMALL_AUDIO_MEMORY_KB = 100        // Small audio buffer
    const val MEDIUM_AUDIO_MEMORY_KB = 1024      // Medium audio buffer
    const val LARGE_AUDIO_MEMORY_KB = 10240      // Large audio buffer
    
    /**
     * Validates if a duration is within the specified baseline
     */
    fun validateBaseline(actualMs: Long, baselineMs: Long, operation: String) {
        if (actualMs > baselineMs) {
            throw AssertionError(
                "$operation took ${actualMs}ms, exceeds ${baselineMs}ms baseline"
            )
        }
    }
    
    /**
     * Logs performance metrics for analysis
     */
    fun logPerformance(operation: String, durationMs: Long, baseline: Long) {
        val percentage = (durationMs.toDouble() / baseline.toDouble() * 100).toInt()
        println("PERFORMANCE: $operation took ${durationMs}ms (${percentage}% of ${baseline}ms baseline)")
    }
}