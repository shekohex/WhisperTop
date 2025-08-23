package me.shadykhalifa.whispertop.presentation.models

import org.junit.Test
import org.junit.Assert.*

class TranscriptionDisplayModelTest {

    @Test
    fun `toDisplayModel truncates text correctly`() {
        val longText = "This is a very long transcription text that exceeds the 47 character limit and should be truncated with ellipsis"
        val model = longText.toDisplayModel()
        
        assertEquals("This is a very long transcription text that exc...", model.previewText)
        assertEquals(longText, model.fullText)
    }

    @Test
    fun `toDisplayModel preserves short text`() {
        val shortText = "Short text"
        val model = shortText.toDisplayModel()
        
        assertEquals("Short text", model.previewText)
        assertEquals("Short text", model.fullText)
    }

    @Test
    fun `toDisplayModel handles exactly 47 characters`() {
        val exactText = "12345678901234567890123456789012345678901234567"
        println("String length: ${exactText.length}")
        assertEquals(47, exactText.length)
        
        val model = exactText.toDisplayModel()
        
        assertEquals("12345678901234567890123456789012345678901234567", model.previewText)
        assertEquals("12345678901234567890123456789012345678901234567", model.fullText)
    }

    @Test
    fun `toDisplayModel handles empty string`() {
        val emptyText = ""
        val model = emptyText.toDisplayModel()
        
        assertEquals("", model.previewText)
        assertEquals("", model.fullText)
    }

    @Test
    fun `toDisplayModel handles unicode characters`() {
        val unicodeText = "Hello 🌍! This contains émojis and spëcial chars!"
        val model = unicodeText.toDisplayModel()
        
        assertEquals("Hello 🌍! This contains émojis and spëcial char...", model.previewText)
        assertEquals(unicodeText, model.fullText)
    }

    @Test
    fun `toDisplayModel sets insertion status correctly`() {
        val text = "Test text"
        val model = text.toDisplayModel(
            insertionStatus = TextInsertionStatus.Completed,
            errorMessage = null
        )
        
        assertEquals(TextInsertionStatus.Completed, model.insertionStatus)
        assertNull(model.errorMessage)
    }

    @Test
    fun `toDisplayModel sets error message correctly`() {
        val text = "Test text"
        val errorMessage = "Failed to insert text"
        val model = text.toDisplayModel(
            insertionStatus = TextInsertionStatus.Failed,
            errorMessage = errorMessage
        )
        
        assertEquals(TextInsertionStatus.Failed, model.insertionStatus)
        assertEquals(errorMessage, model.errorMessage)
    }

    @Test
    fun `TextInsertionStatus enum has all required values`() {
        assertNotNull(TextInsertionStatus.NotStarted)
        assertNotNull(TextInsertionStatus.InProgress)
        assertNotNull(TextInsertionStatus.Completed)
        assertNotNull(TextInsertionStatus.Failed)
    }
}