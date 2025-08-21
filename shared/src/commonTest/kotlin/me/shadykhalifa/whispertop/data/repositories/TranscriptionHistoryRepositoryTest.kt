package me.shadykhalifa.whispertop.data.repositories

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptionHistoryRepositoryTest {
    
    private lateinit var database: AppDatabase
    private lateinit var repository: TranscriptionHistoryRepositoryImpl

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .build()
        repository = TranscriptionHistoryRepositoryImpl(database.transcriptionHistoryDao())
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun saveAndRetrieveTranscription() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        
        val saveResult = repository.saveTranscription(transcription)
        assertTrue(saveResult is Result.Success)
        
        val retrieveResult = repository.getTranscription("1")
        assertTrue(retrieveResult is Result.Success)
        assertNotNull(retrieveResult.data)
        assertEquals(transcription.text, retrieveResult.data.text)
    }

    @Test
    fun updateTranscription() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        repository.saveTranscription(transcription)
        
        val updated = transcription.copy(text = "Updated text")
        val updateResult = repository.updateTranscription(updated)
        assertTrue(updateResult is Result.Success)
        
        val retrieveResult = repository.getTranscription("1")
        assertTrue(retrieveResult is Result.Success)
        assertEquals("Updated text", retrieveResult.data?.text)
    }

    @Test
    fun deleteTranscription() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        repository.saveTranscription(transcription)
        
        val deleteResult = repository.deleteTranscription("1")
        assertTrue(deleteResult is Result.Success)
        
        val retrieveResult = repository.getTranscription("1")
        assertTrue(retrieveResult is Result.Success)
        assertNull(retrieveResult.data)
    }

    @Test
    fun getStatistics() = runTest {
        val transcriptions = listOf(
            createTestTranscription("1", "Hello", duration = 5.0f, confidence = 0.9f, wordCount = 1),
            createTestTranscription("2", "World", duration = 3.0f, confidence = 0.8f, wordCount = 1)
        )
        
        transcriptions.forEach { repository.saveTranscription(it) }
        
        val countResult = repository.getTranscriptionsCount()
        val durationResult = repository.getTotalDuration()
        val confidenceResult = repository.getAverageConfidence()
        val wordsResult = repository.getTotalWordCount()
        
        assertTrue(countResult is Result.Success)
        assertTrue(durationResult is Result.Success)
        assertTrue(confidenceResult is Result.Success)
        assertTrue(wordsResult is Result.Success)
        
        assertEquals(2L, countResult.data)
        assertEquals(8.0f, durationResult.data)
        assertEquals(0.85f, confidenceResult.data)
        assertEquals(2L, wordsResult.data)
    }

    @Test
    fun getAllTranscriptionsFlow() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        repository.saveTranscription(transcription)
        
        val flow = repository.getAllTranscriptions()
        val results = flow.first()
        
        assertEquals(1, results.size)
        assertEquals("Hello world", results.first().text)
    }

    @Test
    fun getRecentTranscriptions() = runTest {
        val transcriptions = (1..25).map { 
            createTestTranscription("$it", "Text $it", timestamp = System.currentTimeMillis() + it)
        }
        
        transcriptions.forEach { repository.saveTranscription(it) }
        
        val recentResult = repository.getRecentTranscriptions(10)
        assertTrue(recentResult is Result.Success)
        assertEquals(10, recentResult.data.size)
        
        // Should be in descending order by timestamp (most recent first)
        assertTrue(recentResult.data[0].timestamp >= recentResult.data[1].timestamp)
    }

    private fun createTestTranscription(
        id: String,
        text: String,
        timestamp: Long = System.currentTimeMillis(),
        duration: Float? = null,
        confidence: Float? = null,
        wordCount: Int = text.split(" ").size
    ) = TranscriptionHistory(
        id = id,
        text = text,
        timestamp = timestamp,
        duration = duration,
        confidence = confidence,
        wordCount = wordCount,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}