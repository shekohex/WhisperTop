package me.shadykhalifa.whispertop.presentation.models

import org.junit.Test
import org.junit.Assert.*

class RecordingStatusTest {

    @Test
    fun `RecordingStatus enum has all required values`() {
        assertNotNull(RecordingStatus.Idle)
        assertNotNull(RecordingStatus.Recording)
        assertNotNull(RecordingStatus.Processing)
        assertNotNull(RecordingStatus.InsertingText)
        assertNotNull(RecordingStatus.Success)
        assertNotNull(RecordingStatus.Error)
    }

    @Test
    fun `RecordingStatus enum values are distinct`() {
        val values = RecordingStatus.entries.toSet()
        
        assertEquals(6, values.size)
        assertTrue(values.contains(RecordingStatus.Idle))
        assertTrue(values.contains(RecordingStatus.Recording))
        assertTrue(values.contains(RecordingStatus.Processing))
        assertTrue(values.contains(RecordingStatus.InsertingText))
        assertTrue(values.contains(RecordingStatus.Success))
        assertTrue(values.contains(RecordingStatus.Error))
    }
}