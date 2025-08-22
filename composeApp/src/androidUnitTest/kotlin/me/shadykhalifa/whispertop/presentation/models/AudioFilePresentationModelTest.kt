package me.shadykhalifa.whispertop.presentation.models

import me.shadykhalifa.whispertop.domain.models.AudioFile
import org.junit.Test
import org.junit.Assert.*

class AudioFilePresentationModelTest {

    @Test
    fun `toPresentationModel formats duration correctly`() {
        val audioFile = AudioFile("test.wav", 90000L, 1024L, "session1")
        val model = audioFile.toPresentationModel()
        
        assertEquals("1:30", model.durationText)
    }

    @Test
    fun `toPresentationModel formats zero duration`() {
        val audioFile = AudioFile("test.wav", 0L, 1024L, null)
        val model = audioFile.toPresentationModel()
        
        assertEquals("0:00", model.durationText)
    }

    @Test
    fun `toPresentationModel formats file size correctly`() {
        val smallFile = AudioFile("test.wav", 5000L, 512L, null)
        val kilobyteFile = AudioFile("test.wav", 5000L, 1536L, null)
        val megabyteFile = AudioFile("test.wav", 5000L, 1572864L, null)
        
        assertEquals("512 B", smallFile.toPresentationModel().fileSizeText)
        assertEquals("1.5 KB", kilobyteFile.toPresentationModel().fileSizeText)
        assertEquals("1.5 MB", megabyteFile.toPresentationModel().fileSizeText)
    }

    @Test
    fun `toPresentationModel determines validity correctly`() {
        val validFile = AudioFile("test.wav", 5000L, 1024L, null)
        val invalidPath = AudioFile("", 5000L, 1024L, null)
        val invalidDuration = AudioFile("test.wav", 0L, 1024L, null)
        val invalidSize = AudioFile("test.wav", 5000L, 0L, null)
        
        assertTrue(validFile.toPresentationModel().isValid)
        assertFalse(invalidPath.toPresentationModel().isValid)
        assertFalse(invalidDuration.toPresentationModel().isValid)
        assertFalse(invalidSize.toPresentationModel().isValid)
    }

    @Test
    fun `toPresentationModel preserves all fields`() {
        val audioFile = AudioFile("path/test.wav", 45000L, 2048L, "session123")
        val model = audioFile.toPresentationModel()
        
        assertEquals("path/test.wav", model.path)
        assertEquals("session123", model.sessionId)
        assertEquals("0:45", model.durationText)
        assertEquals("2.0 KB", model.fileSizeText)
        assertTrue(model.isValid)
    }

    @Test
    fun `formatDuration handles edge cases`() {
        val longFile = AudioFile("test.wav", 3661000L, 1024L, null) // 1 hour 1 minute 1 second
        val model = longFile.toPresentationModel()
        
        assertEquals("61:01", model.durationText)
    }

    @Test
    fun `formatFileSize handles large files`() {
        val gbFile = AudioFile("test.wav", 5000L, 1073741824L, null) // 1GB
        val model = gbFile.toPresentationModel()
        
        assertEquals("1.0 GB", model.fileSizeText)
    }
}