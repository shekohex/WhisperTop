package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TranscriptionSessionTest {

    private val json = Json

    @Test
    fun `should serialize and deserialize TranscriptionSession correctly`() {
        val session = TranscriptionSession(
            id = "test-id-123",
            timestamp = Instant.parse("2023-12-01T10:30:00Z"),
            audioLengthMs = 5000L,
            wordCount = 25,
            characterCount = 150,
            transcribedText = "This is a test transcription"
        )

        val jsonString = json.encodeToString(session)
        assertNotNull(jsonString)
        
        val deserializedSession = json.decodeFromString<TranscriptionSession>(jsonString)
        
        assertEquals(session.id, deserializedSession.id)
        assertEquals(session.timestamp, deserializedSession.timestamp)
        assertEquals(session.audioLengthMs, deserializedSession.audioLengthMs)
        assertEquals(session.wordCount, deserializedSession.wordCount)
        assertEquals(session.characterCount, deserializedSession.characterCount)
        assertEquals(session.transcribedText, deserializedSession.transcribedText)
    }

    @Test
    fun `should handle empty transcribed text`() {
        val session = TranscriptionSession(
            id = "empty-text",
            timestamp = Instant.parse("2023-12-01T10:30:00Z"),
            audioLengthMs = 1000L,
            wordCount = 0,
            characterCount = 0,
            transcribedText = ""
        )

        val jsonString = json.encodeToString(session)
        val deserializedSession = json.decodeFromString<TranscriptionSession>(jsonString)
        
        assertEquals("", deserializedSession.transcribedText)
        assertEquals(0, deserializedSession.wordCount)
        assertEquals(0, deserializedSession.characterCount)
    }

    @Test
    fun `should handle long transcriptions`() {
        val longText = "Lorem ipsum ".repeat(100)
        val session = TranscriptionSession(
            id = "long-text",
            timestamp = Instant.parse("2023-12-01T10:30:00Z"),
            audioLengthMs = 60000L,
            wordCount = 200,
            characterCount = longText.length,
            transcribedText = longText
        )

        val jsonString = json.encodeToString(session)
        val deserializedSession = json.decodeFromString<TranscriptionSession>(jsonString)
        
        assertEquals(longText, deserializedSession.transcribedText)
        assertEquals(200, deserializedSession.wordCount)
    }
}