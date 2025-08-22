package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UserStatisticsModelTest {

    private val json = Json

    @Test
    fun `should serialize and deserialize UserStatistics with new fields`() {
        val statistics = UserStatistics(
            id = "user-123",
            totalWords = 5000L,
            totalSessions = 50,
            totalSpeakingTimeMs = 180000L, // 3 minutes
            averageWordsPerMinute = 120.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 65,
            totalTranscriptions = 45L,
            totalDuration = 180.0f,
            averageAccuracy = 0.95f,
            dailyUsageCount = 10L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = 1701432000000L,
            updatedAt = 1701432000000L
        )

        val jsonString = json.encodeToString(statistics)
        assertNotNull(jsonString)
        
        val deserializedStats = json.decodeFromString<UserStatistics>(jsonString)
        
        assertEquals(statistics.id, deserializedStats.id)
        assertEquals(statistics.totalWords, deserializedStats.totalWords)
        assertEquals(statistics.totalSessions, deserializedStats.totalSessions)
        assertEquals(statistics.totalSpeakingTimeMs, deserializedStats.totalSpeakingTimeMs)
        assertEquals(statistics.averageWordsPerMinute, deserializedStats.averageWordsPerMinute)
        assertEquals(statistics.averageWordsPerSession, deserializedStats.averageWordsPerSession)
        assertEquals(statistics.userTypingWpm, deserializedStats.userTypingWpm)
        assertEquals(statistics.totalTranscriptions, deserializedStats.totalTranscriptions)
        assertEquals(statistics.totalDuration, deserializedStats.totalDuration)
        assertEquals(statistics.averageAccuracy, deserializedStats.averageAccuracy)
        assertEquals(statistics.dailyUsageCount, deserializedStats.dailyUsageCount)
        assertEquals(statistics.mostUsedLanguage, deserializedStats.mostUsedLanguage)
        assertEquals(statistics.mostUsedModel, deserializedStats.mostUsedModel)
        assertEquals(statistics.createdAt, deserializedStats.createdAt)
        assertEquals(statistics.updatedAt, deserializedStats.updatedAt)
    }

    @Test
    fun `should handle null optional fields`() {
        val statistics = UserStatistics(
            id = "user-456",
            totalWords = 0L,
            totalSessions = 0,
            totalSpeakingTimeMs = 0L,
            averageWordsPerMinute = 0.0,
            averageWordsPerSession = 0.0,
            userTypingWpm = 0,
            totalTranscriptions = 0L,
            totalDuration = 0.0f,
            averageAccuracy = null,
            dailyUsageCount = 0L,
            mostUsedLanguage = null,
            mostUsedModel = null,
            createdAt = 1701432000000L,
            updatedAt = 1701432000000L
        )

        val jsonString = json.encodeToString(statistics)
        val deserializedStats = json.decodeFromString<UserStatistics>(jsonString)
        
        assertEquals(null, deserializedStats.averageAccuracy)
        assertEquals(null, deserializedStats.mostUsedLanguage)
        assertEquals(null, deserializedStats.mostUsedModel)
    }

    @Test
    fun `should handle large statistical values`() {
        val statistics = UserStatistics(
            id = "power-user",
            totalWords = 1000000L, // 1 million words
            totalSessions = 10000,
            totalSpeakingTimeMs = 36000000L, // 10 hours
            averageWordsPerMinute = 250.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 120,
            totalTranscriptions = 9500L,
            totalDuration = 36000.0f,
            averageAccuracy = 0.99f,
            dailyUsageCount = 365L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = 1701432000000L,
            updatedAt = 1701432000000L
        )

        val jsonString = json.encodeToString(statistics)
        val deserializedStats = json.decodeFromString<UserStatistics>(jsonString)
        
        assertEquals(1000000L, deserializedStats.totalWords)
        assertEquals(10000, deserializedStats.totalSessions)
        assertEquals(36000000L, deserializedStats.totalSpeakingTimeMs)
        assertEquals(250.0, deserializedStats.averageWordsPerMinute)
        assertEquals(120, deserializedStats.userTypingWpm)
    }

    @Test
    fun `should maintain backwards compatibility with existing fields`() {
        val statistics = UserStatistics(
            id = "legacy-user",
            totalWords = 1000L,
            totalSessions = 10,
            totalSpeakingTimeMs = 60000L,
            averageWordsPerMinute = 100.0,
            averageWordsPerSession = 100.0,
            userTypingWpm = 60,
            totalTranscriptions = 8L,
            totalDuration = 60.0f,
            averageAccuracy = 0.90f,
            dailyUsageCount = 5L,
            mostUsedLanguage = "es",
            mostUsedModel = "whisper-1",
            createdAt = 1701432000000L,
            updatedAt = 1701432000000L
        )

        val jsonString = json.encodeToString(statistics)
        val deserializedStats = json.decodeFromString<UserStatistics>(jsonString)
        
        // Verify legacy fields still work
        assertEquals(statistics.totalTranscriptions, deserializedStats.totalTranscriptions)
        assertEquals(statistics.totalDuration, deserializedStats.totalDuration)
        assertEquals(statistics.averageAccuracy, deserializedStats.averageAccuracy)
        assertEquals(statistics.dailyUsageCount, deserializedStats.dailyUsageCount)
        assertEquals(statistics.mostUsedLanguage, deserializedStats.mostUsedLanguage)
        assertEquals(statistics.mostUsedModel, deserializedStats.mostUsedModel)
    }
}