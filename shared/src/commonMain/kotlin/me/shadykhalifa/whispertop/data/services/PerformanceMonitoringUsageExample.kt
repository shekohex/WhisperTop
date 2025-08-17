package me.shadykhalifa.whispertop.data.services

import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.domain.services.MetricsExportFormat
// Note: PerformanceMonitor is Android-specific, this example demonstrates the concept
import java.util.UUID

/**
 * Example usage of the performance monitoring system for WhisperTop.
 * This demonstrates how to integrate performance metrics collection
 * into recording and transcription workflows.
 */
class PerformanceMonitoringUsageExample(
    private val metricsCollector: MetricsCollector
) {
    
    suspend fun demonstrateRecordingMetrics() {
        // Start a new performance session
        val sessionId = UUID.randomUUID().toString()
        metricsCollector.startSession(sessionId)
        
        // Start recording metrics
        metricsCollector.startRecordingMetrics(sessionId)
        
        // Record memory snapshots during recording
        metricsCollector.recordMemorySnapshot(sessionId, "recording_start")
        
        // Simulate recording operations with timing
        // Note: PerformanceMonitor.TimingMonitor would be used here in Android context
        kotlinx.coroutines.delay(100) // Simulate audio setup time
        
        // Update recording metrics with audio parameters
        metricsCollector.updateRecordingMetrics(sessionId) { metrics ->
            metrics.copy(
                sampleRate = 16000,
                channels = 1,
                bitRate = 16
            )
        }
        
        // Simulate periodic memory monitoring during recording
        repeat(3) {
            kotlinx.coroutines.delay(1000) // Wait 1 second
            metricsCollector.recordMemorySnapshot(sessionId, "recording_progress_${it + 1}")
        }
        
        // End recording with success
        metricsCollector.endRecordingMetrics(sessionId, true)
        
        // End session
        metricsCollector.endSession(sessionId)
        
        println("Recording metrics demonstration completed for session: $sessionId")
    }
    
    suspend fun demonstrateTranscriptionMetrics() {
        // Start a new performance session
        val sessionId = UUID.randomUUID().toString()
        metricsCollector.startSession(sessionId)
        
        // Start transcription metrics
        metricsCollector.startTranscriptionMetrics(sessionId)
        
        // Update with initial request data
        metricsCollector.updateTranscriptionMetrics(sessionId) { metrics ->
            metrics.copy(
                model = "whisper-1",
                language = "en",
                audioFileSize = 1024 * 1024, // 1MB
                audioFileDuration = 30000 // 30 seconds
            )
        }
        
        // Simulate API call timing
        val transcriptionResult = run {
            // Simulate network request time
            kotlinx.coroutines.delay(2000) // 2 seconds
            "This is a test transcription result"
        }
        
        // Update with final response data
        metricsCollector.updateTranscriptionMetrics(sessionId) { metrics ->
            metrics.copy(
                transcriptionLength = transcriptionResult.length,
                networkResponseSize = transcriptionResult.length.toLong() * 2, // Approximate JSON overhead
                connectionTimeMs = 500,
                transferTimeMs = 1500
            )
        }
        
        // End transcription with success
        metricsCollector.endTranscriptionMetrics(sessionId, true)
        
        // End session
        metricsCollector.endSession(sessionId)
        
        println("Transcription metrics demonstration completed for session: $sessionId")
    }
    
    suspend fun demonstrateMetricsAggregation() {
        val now = System.currentTimeMillis()
        val oneDayAgo = now - (24 * 60 * 60 * 1000)
        
        // Get metrics aggregation for the last 24 hours
        val aggregation = metricsCollector.getMetricsAggregation(oneDayAgo, now)
        
        println("Performance Metrics Summary (Last 24 Hours):")
        println("Total Sessions: ${aggregation.totalSessions}")
        println("Successful Sessions: ${aggregation.successfulSessions}")
        println("Success Rate: ${aggregation.getSuccessRate()}%")
        println("Average Recording Duration: ${aggregation.averageRecordingDuration}ms")
        println("Average Transcription Time: ${aggregation.averageTranscriptionTime}ms")
        println("Peak Memory Usage: ${aggregation.peakMemoryUsage / 1024 / 1024}MB")
        println("API Error Rate: ${aggregation.getApiErrorRate()}%")
        println("Performance Warnings: ${aggregation.performanceWarnings.size}")
        
        if (aggregation.commonErrors.isNotEmpty()) {
            println("\nMost Common Errors:")
            aggregation.commonErrors.forEach { (error, count) ->
                println("  $error: $count occurrences")
            }
        }
    }
    
    suspend fun demonstrateMetricsExport() {
        // Export metrics in JSON format
        val jsonMetrics = metricsCollector.exportMetrics(MetricsExportFormat.JSON)
        println("JSON Export Sample (first 500 chars):")
        println(jsonMetrics.take(500) + if (jsonMetrics.length > 500) "..." else "")
        
        // Export metrics in CSV format
        val csvMetrics = metricsCollector.exportMetrics(MetricsExportFormat.CSV)
        println("\nCSV Export Sample:")
        println(csvMetrics.lines().take(5).joinToString("\n"))
    }
    
    suspend fun demonstrateMemoryMonitoring() {
        val sessionId = UUID.randomUUID().toString()
        metricsCollector.startSession(sessionId)
        
        // Demonstrate memory monitoring with different contexts
        // Note: PerformanceMonitor.MemoryMonitor would be used here in Android context
        metricsCollector.recordMemorySnapshot(sessionId, "startup")
        
        // Simulate memory-intensive operation
        val largeList = mutableListOf<ByteArray>()
        repeat(100) {
            largeList.add(ByteArray(1024 * 10)) // 10KB each
            if (it % 20 == 0) {
                metricsCollector.recordMemorySnapshot(sessionId, "memory_test_$it")
            }
        }
        
        println("Memory monitoring demonstration completed")
        
        metricsCollector.endSession(sessionId)
    }
    
    suspend fun runFullDemo() {
        println("=== WhisperTop Performance Monitoring Demo ===\n")
        
        demonstrateRecordingMetrics()
        println()
        
        demonstrateTranscriptionMetrics()
        println()
        
        demonstrateMemoryMonitoring()
        println()
        
        demonstrateMetricsAggregation()
        println()
        
        demonstrateMetricsExport()
        println()
        
        println("=== Performance Monitoring Demo Complete ===")
    }
}