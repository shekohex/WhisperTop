package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DailyUsageTest {

    private val json = Json

    @Test
    fun `should serialize and deserialize DailyUsage correctly`() {
        val dailyUsage = DailyUsage(
            date = LocalDate(2023, 12, 1),
            sessionsCount = 5,
            wordsTranscribed = 250L,
            totalTimeMs = 30000L
        )

        val jsonString = json.encodeToString(dailyUsage)
        assertNotNull(jsonString)
        
        val deserializedUsage = json.decodeFromString<DailyUsage>(jsonString)
        
        assertEquals(dailyUsage.date, deserializedUsage.date)
        assertEquals(dailyUsage.sessionsCount, deserializedUsage.sessionsCount)
        assertEquals(dailyUsage.wordsTranscribed, deserializedUsage.wordsTranscribed)
        assertEquals(dailyUsage.totalTimeMs, deserializedUsage.totalTimeMs)
    }

    @Test
    fun `should handle zero usage values`() {
        val dailyUsage = DailyUsage(
            date = LocalDate(2023, 12, 1),
            sessionsCount = 0,
            wordsTranscribed = 0L,
            totalTimeMs = 0L
        )

        val jsonString = json.encodeToString(dailyUsage)
        val deserializedUsage = json.decodeFromString<DailyUsage>(jsonString)
        
        assertEquals(0, deserializedUsage.sessionsCount)
        assertEquals(0L, deserializedUsage.wordsTranscribed)
        assertEquals(0L, deserializedUsage.totalTimeMs)
    }

    @Test
    fun `should handle large usage numbers`() {
        val dailyUsage = DailyUsage(
            date = LocalDate(2023, 12, 1),
            sessionsCount = 100,
            wordsTranscribed = 50000L,
            totalTimeMs = 7200000L // 2 hours
        )

        val jsonString = json.encodeToString(dailyUsage)
        val deserializedUsage = json.decodeFromString<DailyUsage>(jsonString)
        
        assertEquals(100, deserializedUsage.sessionsCount)
        assertEquals(50000L, deserializedUsage.wordsTranscribed)
        assertEquals(7200000L, deserializedUsage.totalTimeMs)
    }

    @Test
    fun `should handle different date formats`() {
        val dates = listOf(
            LocalDate(2023, 1, 1),    // New Year
            LocalDate(2023, 2, 29),   // Leap year (this should fail, but test framework)
            LocalDate(2023, 12, 31)   // Year end
        )

        dates.forEach { date ->
            val dailyUsage = DailyUsage(
                date = date,
                sessionsCount = 1,
                wordsTranscribed = 10L,
                totalTimeMs = 1000L
            )

            val jsonString = json.encodeToString(dailyUsage)
            val deserializedUsage = json.decodeFromString<DailyUsage>(jsonString)
            
            assertEquals(date, deserializedUsage.date)
        }
    }
}