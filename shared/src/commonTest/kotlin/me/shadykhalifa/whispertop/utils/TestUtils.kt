package me.shadykhalifa.whispertop.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlin.time.Duration.Companion.seconds

object TestUtils {
    
    // Coroutines testing utilities
    @OptIn(ExperimentalCoroutinesApi::class)
    fun createTestScope(
        dispatcher: TestDispatcher = UnconfinedTestDispatcher()
    ): TestScope = TestScope(dispatcher)
    
    @OptIn(ExperimentalCoroutinesApi::class)
    fun createTestScheduler(): TestCoroutineScheduler = TestCoroutineScheduler()
    
    // Flow testing utilities
    suspend fun <T> Flow<T>.collectFirstValue(): T {
        return withTimeout(5.seconds) {
            first()
        }
    }
    
    suspend fun <T> Flow<T>.collectAllValues(): List<T> {
        return withTimeout(10.seconds) {
            toList()
        }
    }
    
    suspend fun <T> Flow<T>.collectValuesWithTimeout(
        timeout: kotlin.time.Duration = 5.seconds
    ): List<T> {
        return withTimeout(timeout) {
            toList()
        }
    }
    
    // Time-based testing utilities
    @OptIn(ExperimentalCoroutinesApi::class)
    fun TestScope.advanceTimeAndIdle(timeMs: Long) {
        advanceTimeBy(timeMs)
        advanceUntilIdle()
    }
    
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    
    fun timeAgo(minutesAgo: Long): Long = 
        currentTimeMillis() - (minutesAgo * 60 * 1000)
    
    fun timeFromNow(minutesFromNow: Long): Long = 
        currentTimeMillis() + (minutesFromNow * 60 * 1000)
    
    // Assertion utilities
    fun assertWithinRange(actual: Float, expected: Float, tolerance: Float = 0.01f) {
        if (kotlin.math.abs(actual - expected) > tolerance) {
            throw AssertionError(
                "Expected $actual to be within $tolerance of $expected, " +
                "but difference was ${kotlin.math.abs(actual - expected)}"
            )
        }
    }
    
    fun assertWithinRange(actual: Double, expected: Double, tolerance: Double = 0.01) {
        if (kotlin.math.abs(actual - expected) > tolerance) {
            throw AssertionError(
                "Expected $actual to be within $tolerance of $expected, " +
                "but difference was ${kotlin.math.abs(actual - expected)}"
            )
        }
    }
    
    fun assertTimestampWithinLast(timestamp: Long, minutes: Long = 5) {
        val cutoff = currentTimeMillis() - (minutes * 60 * 1000)
        if (timestamp < cutoff) {
            throw AssertionError(
                "Timestamp $timestamp is older than $minutes minutes ago ($cutoff)"
            )
        }
    }
    
    fun assertListsEquivalent(
        actual: List<*>,
        expected: List<*>,
        message: String = "Lists are not equivalent"
    ) {
        if (actual.size != expected.size) {
            throw AssertionError("$message - Size mismatch: ${actual.size} vs ${expected.size}")
        }
        
        actual.forEachIndexed { index, item ->
            if (item != expected[index]) {
                throw AssertionError(
                    "$message - Item mismatch at index $index: $item vs ${expected[index]}"
                )
            }
        }
    }
    
    // Performance testing utilities
    inline fun measureTimeMillis(block: () -> Unit): Long {
        val start = currentTimeMillis()
        block()
        return currentTimeMillis() - start
    }
    
    suspend inline fun measureTimeMillisSuspend(block: suspend () -> Unit): Long {
        val start = currentTimeMillis()
        block()
        return currentTimeMillis() - start
    }
    
    fun assertPerformanceWithin(
        actualMs: Long,
        maxExpectedMs: Long,
        operation: String = "Operation"
    ) {
        if (actualMs > maxExpectedMs) {
            throw AssertionError(
                "$operation took ${actualMs}ms, expected maximum ${maxExpectedMs}ms"
            )
        }
    }
    
    // Memory testing utilities
    fun forceGarbageCollection() {
        repeat(3) {
            System.gc()
            Thread.sleep(10)
        }
    }
    
    fun getMemoryUsage(): Long {
        forceGarbageCollection()
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
    
    // Data validation utilities
    fun validateAudioDuration(duration: Float?) {
        duration?.let {
            if (it < 0) {
                throw AssertionError("Audio duration cannot be negative: $it")
            }
            if (it > 86400) { // 24 hours
                throw AssertionError("Audio duration seems unreasonably long: $it seconds")
            }
        }
    }
    
    fun validateConfidenceScore(confidence: Float?) {
        confidence?.let {
            if (it < 0f || it > 1f) {
                throw AssertionError("Confidence score must be between 0 and 1: $it")
            }
        }
    }
    
    fun validateTimestamp(timestamp: Long) {
        val year2000 = 946684800000L // Jan 1, 2000 in milliseconds
        val year2100 = 4102444800000L // Jan 1, 2100 in milliseconds
        
        if (timestamp < year2000 || timestamp > year2100) {
            throw AssertionError(
                "Timestamp seems unreasonable: $timestamp (not between 2000-2100)"
            )
        }
    }
    
    fun validateNonEmptyString(value: String?, fieldName: String) {
        if (value.isNullOrBlank()) {
            throw AssertionError("$fieldName cannot be null or empty")
        }
    }
    
    // Mock data utilities
    fun createRandomString(length: Int = 10): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..length).map { chars.random() }.joinToString("")
    }
    
    fun createRandomFloat(min: Float = 0f, max: Float = 1f): Float {
        return min + (kotlin.random.Random.nextFloat() * (max - min))
    }
    
    fun createRandomInt(min: Int = 0, max: Int = 100): Int {
        return kotlin.random.Random.nextInt(min, max + 1)
    }
    
    fun createRandomBoolean(): Boolean = kotlin.random.Random.nextBoolean()
    
    // File and path utilities
    fun createTempFilePath(extension: String = "wav"): String {
        return "/tmp/test_${createRandomString(8)}.$extension"
    }
    
    fun validateFilePath(path: String) {
        if (path.isBlank()) {
            throw AssertionError("File path cannot be empty")
        }
        if (!path.contains("/") && !path.contains("\\")) {
            throw AssertionError("File path should contain directory separators: $path")
        }
    }
    
    // Collection utilities
    fun <T> List<T>.assertSize(expectedSize: Int, message: String = "List size mismatch") {
        if (size != expectedSize) {
            throw AssertionError("$message - Expected $expectedSize but got $size")
        }
    }
    
    fun <T> List<T>.assertNotEmpty(message: String = "List should not be empty") {
        if (isEmpty()) {
            throw AssertionError(message)
        }
    }
    
    fun <T, R : Comparable<R>> List<T>.assertSortedBy(selector: (T) -> R, descending: Boolean = false) {
        if (size <= 1) return
        
        for (i in 1 until size) {
            val prev = selector(this[i - 1])
            val curr = selector(this[i])
            
            val comparison = if (descending) {
                prev < curr
            } else {
                prev > curr
            }
            
            if (comparison) {
                throw AssertionError(
                    "List is not properly sorted at index $i: ${this[i-1]} vs ${this[i]}"
                )
            }
        }
    }
    
    // Test environment utilities
    fun isRunningInCI(): Boolean {
        return System.getenv("CI") == "true" || 
               System.getenv("GITHUB_ACTIONS") == "true" ||
               System.getenv("JENKINS_URL") != null
    }
    
    fun getTestTimeout(): kotlin.time.Duration {
        return if (isRunningInCI()) {
            30.seconds // Longer timeout in CI
        } else {
            10.seconds // Shorter timeout locally
        }
    }
    
    // Debug utilities
    fun debugPrint(message: String, enabled: Boolean = true) {
        if (enabled && !isRunningInCI()) {
            println("[TEST DEBUG] $message")
        }
    }
    
    fun debugPrintCollection(
        collection: Collection<*>,
        label: String = "Collection",
        enabled: Boolean = true
    ) {
        if (enabled && !isRunningInCI()) {
            println("[TEST DEBUG] $label (${collection.size} items):")
            collection.forEachIndexed { index, item ->
                println("  [$index] $item")
            }
        }
    }
    
    // Error simulation utilities
    fun simulateNetworkDelay(delayMs: Long = 100) {
        Thread.sleep(delayMs)
    }
    
    suspend fun simulateNetworkDelaySuspend(delayMs: Long = 100) {
        kotlinx.coroutines.delay(delayMs)
    }
    
    class SimulatedNetworkError(message: String) : Exception("Simulated network error: $message")
    class SimulatedApiError(message: String) : Exception("Simulated API error: $message")
    class SimulatedFileSystemError(message: String) : Exception("Simulated filesystem error: $message")
    
    // Test data validation
    fun validateTestData(
        transcriptionHistory: me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
    ) {
        validateNonEmptyString(transcriptionHistory.id, "TranscriptionHistory.id")
        validateNonEmptyString(transcriptionHistory.text, "TranscriptionHistory.text")
        validateTimestamp(transcriptionHistory.timestamp)
        validateAudioDuration(transcriptionHistory.duration)
        validateConfidenceScore(transcriptionHistory.confidence)
        
        if (transcriptionHistory.wordCount < 0) {
            throw AssertionError("Word count cannot be negative: ${transcriptionHistory.wordCount}")
        }
    }
    
    fun validateTestData(
        appSettings: me.shadykhalifa.whispertop.domain.models.AppSettings
    ) {
        validateNonEmptyString(appSettings.apiKey, "AppSettings.apiKey")
        validateNonEmptyString(appSettings.selectedModel, "AppSettings.selectedModel")
        
        if (appSettings.temperature != null) {
            if (appSettings.temperature < 0f || appSettings.temperature > 1f) {
                throw AssertionError("Temperature must be between 0 and 1: ${appSettings.temperature}")
            }
        }
    }
}