package me.shadykhalifa.whispertop.data.serializers

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DateTimeSerializersTest {

    private val json = Json

    @Test
    fun `InstantSerializer should serialize and deserialize Instant correctly`() {
        val instant = Instant.parse("2023-12-01T10:30:00Z")
        
        val serializedInstant = json.encodeToString(InstantSerializer, instant)
        assertNotNull(serializedInstant)
        assertTrue(serializedInstant.contains("2023-12-01T10:30:00Z"))
        
        val deserializedInstant = json.decodeFromString(InstantSerializer, serializedInstant)
        assertEquals(instant, deserializedInstant)
    }

    @Test
    fun `InstantSerializer should handle different time zones`() {
        val instants = listOf(
            Instant.parse("2023-01-01T00:00:00Z"),
            Instant.parse("2023-06-15T12:30:45.123Z"),
            Instant.parse("2023-12-31T23:59:59.999999999Z")
        )

        instants.forEach { instant ->
            val serialized = json.encodeToString(InstantSerializer, instant)
            val deserialized = json.decodeFromString<Instant>(InstantSerializer, serialized)
            assertEquals(instant, deserialized)
        }
    }

    @Test
    fun `LocalDateSerializer should serialize and deserialize LocalDate correctly`() {
        val localDate = LocalDate(2023, 12, 1)
        
        val serializedDate = json.encodeToString(LocalDateSerializer, localDate)
        assertNotNull(serializedDate)
        assertTrue(serializedDate.contains("2023-12-01"))
        
        val deserializedDate = json.decodeFromString(LocalDateSerializer, serializedDate)
        assertEquals(localDate, deserializedDate)
    }

    @Test
    fun `LocalDateSerializer should handle edge dates`() {
        val dates = listOf(
            LocalDate(1970, 1, 1),    // Unix epoch
            LocalDate(2000, 2, 29),   // Leap year
            LocalDate(2023, 12, 31),  // Year end
            LocalDate(2024, 1, 1)     // Year start
        )

        dates.forEach { date ->
            val serialized = json.encodeToString(LocalDateSerializer, date)
            val deserialized = json.decodeFromString<LocalDate>(LocalDateSerializer, serialized)
            assertEquals(date, deserialized)
        }
    }

    @Test
    fun `serializers should produce valid JSON strings`() {
        val instant = Instant.parse("2023-12-01T10:30:00Z")
        val localDate = LocalDate(2023, 12, 1)
        
        val instantJson = json.encodeToString(InstantSerializer, instant)
        val dateJson = json.encodeToString(LocalDateSerializer, localDate)
        
        // Should be valid JSON strings (quoted)
        assertTrue(instantJson.startsWith("\"") && instantJson.endsWith("\""))
        assertTrue(dateJson.startsWith("\"") && dateJson.endsWith("\""))
        
        // Should not contain additional escape characters
        assertEquals("\"2023-12-01T10:30:00Z\"", instantJson)
        assertEquals("\"2023-12-01\"", dateJson)
    }
}