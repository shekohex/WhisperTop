package me.shadykhalifa.whispertop.data.repositories

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptionHistoryRepositoryImplTest {

    private fun createInMemoryDatabase(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            klass = AppDatabase::class.java
        ).build()
    }

    private fun createRepository(database: AppDatabase): TranscriptionHistoryRepositoryImpl {
        return TranscriptionHistoryRepositoryImpl(database.transcriptionHistoryDao())
    }

    @Test
    fun saveTranscription_shouldReturnSuccessWithId() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        val result = repository.saveTranscription(
            text = "Test transcription",
            duration = 5.0f,
            audioFilePath = "/test/path",
            confidence = 0.95f,
            customPrompt = null,
            temperature = 0.5f,
            language = "en",
            model = "whisper-1"
        )

        assertTrue(result is Result.Success)
        assertNotNull(result.data)
        
        database.close()
    }

    @Test
    fun getTranscription_shouldReturnCorrectItem() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        val saveResult = repository.saveTranscription(
            text = "Test transcription",
            duration = 5.0f,
            audioFilePath = null,
            confidence = null,
            customPrompt = null,
            temperature = null,
            language = null,
            model = null
        )

        assertTrue(saveResult is Result.Success)
        val transcriptionId = saveResult.data

        val getResult = repository.getTranscription(transcriptionId)
        assertTrue(getResult is Result.Success)
        
        val transcription = getResult.data
        assertNotNull(transcription)
        assertEquals("Test transcription", transcription.text)
        assertEquals(5.0f, transcription.duration)
        
        database.close()
    }

    @Test
    fun getTranscription_withInvalidId_shouldReturnNull() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        val result = repository.getTranscription("invalid-id")
        
        assertTrue(result is Result.Success)
        assertNull(result.data)
        
        database.close()
    }

    @Test
    fun deleteTranscription_shouldRemoveItem() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        val saveResult = repository.saveTranscription(
            text = "Test transcription",
            duration = null,
            audioFilePath = null,
            confidence = null,
            customPrompt = null,
            temperature = null,
            language = null,
            model = null
        )

        assertTrue(saveResult is Result.Success)
        val transcriptionId = saveResult.data

        val deleteResult = repository.deleteTranscription(transcriptionId)
        assertTrue(deleteResult is Result.Success)

        val getResult = repository.getTranscription(transcriptionId)
        assertTrue(getResult is Result.Success)
        assertNull(getResult.data)
        
        database.close()
    }

    @Test
    fun getAllTranscriptionsFlow_shouldReturnAllItems() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        // Save multiple transcriptions
        val texts = listOf("First", "Second", "Third")
        texts.forEach { text ->
            repository.saveTranscription(
                text = text,
                duration = null,
                audioFilePath = null,
                confidence = null,
                customPrompt = null,
                temperature = null,
                language = null,
                model = null
            )
        }

        val result = repository.getAllTranscriptionsFlow().first()
        assertEquals(3, result.size)
        
        database.close()
    }

    @Test
    fun getRecentTranscriptions_shouldRespectLimit() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        // Save 5 transcriptions
        repeat(5) { index ->
            repository.saveTranscription(
                text = "Transcription $index",
                duration = null,
                audioFilePath = null,
                confidence = null,
                customPrompt = null,
                temperature = null,
                language = null,
                model = null
            )
        }

        val result = repository.getRecentTranscriptions(3)
        assertTrue(result is Result.Success)
        assertEquals(3, result.data.size)
        
        database.close()
    }

    @Test
    fun getTranscriptionStatistics_shouldCalculateCorrectly() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        // Save transcriptions with known values
        repository.saveTranscription(
            text = "First",
            duration = 10.0f,
            audioFilePath = null,
            confidence = 0.9f,
            customPrompt = null,
            temperature = null,
            language = "en",
            model = "whisper-1"
        )

        repository.saveTranscription(
            text = "Second",
            duration = 15.0f,
            audioFilePath = null,
            confidence = 0.8f,
            customPrompt = null,
            temperature = null,
            language = "en",
            model = "whisper-1"
        )

        val result = repository.getTranscriptionStatistics()
        assertTrue(result is Result.Success)
        
        val stats = result.data
        assertEquals(2L, stats.totalCount)
        assertEquals(25.0f, stats.totalDuration)
        assertEquals(0.85f, stats.averageConfidence) // (0.9 + 0.8) / 2
        assertEquals("en", stats.mostUsedLanguage)
        assertEquals("whisper-1", stats.mostUsedModel)
        
        database.close()
    }

    @Test
    fun deleteAllTranscriptions_shouldRemoveAllItems() = runTest {
        val database = createInMemoryDatabase()
        val repository = createRepository(database)

        // Save multiple transcriptions
        repeat(3) { index ->
            repository.saveTranscription(
                text = "Transcription $index",
                duration = null,
                audioFilePath = null,
                confidence = null,
                customPrompt = null,
                temperature = null,
                language = null,
                model = null
            )
        }

        val deleteResult = repository.deleteAllTranscriptions()
        assertTrue(deleteResult is Result.Success)

        val statsResult = repository.getTranscriptionStatistics()
        assertTrue(statsResult is Result.Success)
        assertEquals(0L, statsResult.data.totalCount)
        
        database.close()
    }
}