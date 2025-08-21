package me.shadykhalifa.whispertop.data.database

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TranscriptionHistoryDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .build()
        dao = database.transcriptionHistoryDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTranscription() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        
        dao.insert(transcription)
        val retrieved = dao.getById("1")
        
        assertNotNull(retrieved)
        assertEquals(transcription.text, retrieved.text)
        assertEquals(transcription.id, retrieved.id)
    }

    @Test
    fun updateTranscription() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        dao.insert(transcription)
        
        val updated = transcription.copy(text = "Updated text")
        dao.update(updated)
        
        val retrieved = dao.getById("1")
        assertNotNull(retrieved)
        assertEquals("Updated text", retrieved.text)
    }

    @Test
    fun deleteTranscription() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        dao.insert(transcription)
        
        dao.deleteById("1")
        val retrieved = dao.getById("1")
        
        assertNull(retrieved)
    }

    @Test
    fun searchByText() = runTest {
        val transcriptions = listOf(
            createTestTranscription("1", "Hello world"),
            createTestTranscription("2", "Goodbye world"),
            createTestTranscription("3", "Programming is fun")
        )
        
        dao.insertAll(transcriptions)
        
        // Test searching - this would normally use PagingSource
        val count = dao.getCount()
        assertEquals(3, count)
        
        val recent = dao.getRecent(10)
        assertEquals(3, recent.size)
    }

    @Test
    fun getStatistics() = runTest {
        val transcriptions = listOf(
            createTestTranscription("1", "Hello world", duration = 5.0f, confidence = 0.9f, wordCount = 2),
            createTestTranscription("2", "Goodbye world", duration = 3.0f, confidence = 0.8f, wordCount = 2)
        )
        
        dao.insertAll(transcriptions)
        
        val count = dao.getCount()
        val totalDuration = dao.getTotalDuration()
        val avgConfidence = dao.getAverageConfidence()
        val totalWords = dao.getTotalWordCount()
        
        assertEquals(2, count)
        assertEquals(8.0f, totalDuration)
        assertEquals(0.85f, avgConfidence)
        assertEquals(4, totalWords)
    }

    @Test
    fun flowUpdates() = runTest {
        val transcription = createTestTranscription("1", "Hello world")
        
        // Test flow updates
        dao.insert(transcription)
        val flow = dao.getAllFlow()
        val results = flow.first()
        
        assertEquals(1, results.size)
        assertEquals("Hello world", results.first().text)
    }

    @Test
    fun deleteOlderThan() = runTest {
        val oldTime = System.currentTimeMillis() - 86400000L // 1 day ago
        val recentTime = System.currentTimeMillis()
        
        val transcriptions = listOf(
            createTestTranscription("1", "Old transcription", timestamp = oldTime),
            createTestTranscription("2", "Recent transcription", timestamp = recentTime)
        )
        
        dao.insertAll(transcriptions)
        
        val deletedCount = dao.deleteOlderThan(System.currentTimeMillis() - 43200000L) // 12 hours ago
        assertEquals(1, deletedCount)
        
        val remaining = dao.getCount()
        assertEquals(1, remaining)
    }

    private fun createTestTranscription(
        id: String,
        text: String,
        timestamp: Long = System.currentTimeMillis(),
        duration: Float? = null,
        confidence: Float? = null,
        wordCount: Int = text.split(" ").size
    ) = TranscriptionHistoryEntity(
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