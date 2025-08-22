package me.shadykhalifa.whispertop.data.repositories

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.utils.Result
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TranscriptionHistoryRepositoryImplTest {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: TranscriptionHistoryRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        repository = TranscriptionHistoryRepositoryImpl(database.transcriptionHistoryDao())
    }
    
    @After
    fun tearDown() {
        
    }

    private fun createRepository(database: AppDatabase): TranscriptionHistoryRepositoryImpl {
        return TranscriptionHistoryRepositoryImpl(database.transcriptionHistoryDao())
    }

    @Test
    fun saveTranscription_shouldReturnSuccessWithId() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }

    @Test
    fun getTranscription_shouldReturnCorrectItem() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }

    @Test
    fun getTranscription_withInvalidId_shouldReturnNull() = runTest {
        // Use setUp database
        // Use setUp repository

        val result = repository.getTranscription("invalid-id")
        
        assertTrue(result is Result.Success)
        assertNull(result.data)
        
        
    }

    @Test
    fun deleteTranscription_shouldRemoveItem() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }

    @Test
    fun getAllTranscriptionsFlow_shouldReturnAllItems() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }

    @Test
    fun getRecentTranscriptions_shouldRespectLimit() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }

    @Test
    fun getTranscriptionStatistics_shouldCalculateCorrectly() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }

    @Test
    fun deleteAllTranscriptions_shouldRemoveAllItems() = runTest {
        // Use setUp database
        // Use setUp repository

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
        
        
    }
}