package me.shadykhalifa.whispertop.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class TranscriptionHistoryDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        dao = database.transcriptionHistoryDao()
    }
    
    @After
    fun tearDown() {
        
    }

    private fun createTestEntity(
        id: String = "test-id",
        text: String = "Test transcription",
        timestamp: Long = System.currentTimeMillis()
    ): TranscriptionHistoryEntity {
        return TranscriptionHistoryEntity(
            id = id,
            text = text,
            timestamp = timestamp,
            duration = 5.0f,
            audioFilePath = "/test/path",
            confidence = 0.95f,
            customPrompt = "Test prompt",
            temperature = 0.5f,
            language = "en",
            model = "whisper-1"
        )
    }

    @Test
    fun insertAndRetrieve_shouldWork() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        val retrieved = dao.getById(testEntity.id)

        assertNotNull(retrieved)
        assertEquals(testEntity.id, retrieved.id)
        assertEquals(testEntity.text, retrieved.text)
        assertEquals(testEntity.confidence, retrieved.confidence)
        
        
    }

    @Test
    fun insertAll_shouldInsertMultipleEntities() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("id1", "First transcription"),
            createTestEntity("id2", "Second transcription"),
            createTestEntity("id3", "Third transcription")
        )

        dao.insertAll(entities)
        val count = dao.getCount()

        assertEquals(3L, count)
        
        
    }

    @Test
    fun update_shouldModifyExistingEntity() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        val updatedEntity = testEntity.copy(text = "Updated transcription")
        dao.update(updatedEntity)

        val retrieved = dao.getById(testEntity.id)
        assertNotNull(retrieved)
        assertEquals("Updated transcription", retrieved.text)
        
        
    }

    @Test
    fun delete_shouldRemoveEntity() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        dao.delete(testEntity)

        val retrieved = dao.getById(testEntity.id)
        assertNull(retrieved)
        
        
    }

    @Test
    fun deleteById_shouldRemoveEntityById() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        dao.deleteById(testEntity.id)

        val retrieved = dao.getById(testEntity.id)
        assertNull(retrieved)
        
        
    }

    @Test
    fun getAllFlow_shouldReturnAllEntities() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("id1", "First", 1000L),
            createTestEntity("id2", "Second", 2000L),
            createTestEntity("id3", "Third", 3000L)
        )

        dao.insertAll(entities)
        val retrieved = dao.getAllFlow().first()

        assertEquals(3, retrieved.size)
        // Should be ordered by timestamp DESC
        assertEquals("Third", retrieved[0].text)
        assertEquals("Second", retrieved[1].text)
        assertEquals("First", retrieved[2].text)
        
        
    }

    @Test
    fun getRecent_shouldReturnLimitedResults() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = (1..5).map { index ->
            createTestEntity("id$index", "Transcription $index", index.toLong())
        }

        dao.insertAll(entities)
        val recent = dao.getRecent(3)

        assertEquals(3, recent.size)
        // Should be ordered by timestamp DESC
        assertEquals("Transcription 5", recent[0].text)
        assertEquals("Transcription 4", recent[1].text)
        assertEquals("Transcription 3", recent[2].text)
        
        
    }

    @Test
    fun getTotalDuration_shouldCalculateCorrectly() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("id1").copy(duration = 10.0f),
            createTestEntity("id2").copy(duration = 15.5f),
            createTestEntity("id3").copy(duration = null) // Null should be ignored
        )

        dao.insertAll(entities)
        val totalDuration = dao.getTotalDuration()

        assertEquals(25.5f, totalDuration)
        
        
    }

    @Test
    fun getAverageConfidence_shouldCalculateCorrectly() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("id1").copy(confidence = 0.8f),
            createTestEntity("id2").copy(confidence = 0.9f),
            createTestEntity("id3").copy(confidence = null) // Null should be ignored
        )

        dao.insertAll(entities)
        val avgConfidence = dao.getAverageConfidence()

        assertEquals(0.85f, avgConfidence)
        
        
    }

    @Test
    fun getMostUsedLanguage_shouldReturnCorrectLanguage() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("id1").copy(language = "en"),
            createTestEntity("id2").copy(language = "en"),
            createTestEntity("id3").copy(language = "es"),
            createTestEntity("id4").copy(language = "fr"),
            createTestEntity("id5").copy(language = "en")
        )

        dao.insertAll(entities)
        val mostUsedLanguage = dao.getMostUsedLanguage()

        assertEquals("en", mostUsedLanguage?.language)
        
        
    }

    @Test
    fun getDailyTranscriptionCount_shouldCountCorrectly() = runTest {
        // Use setUp database
        // Use setUp dao
        val currentTime = System.currentTimeMillis()
        val startOfDay = currentTime - (currentTime % (24 * 60 * 60 * 1000))
        
        val entities = listOf(
            createTestEntity("id1", timestamp = startOfDay + 1000), // Today
            createTestEntity("id2", timestamp = startOfDay + 2000), // Today
            createTestEntity("id3", timestamp = startOfDay - 1000)  // Yesterday
        )

        dao.insertAll(entities)
        val dailyCount = dao.getDailyTranscriptionCount(startOfDay)

        assertEquals(2L, dailyCount)
        
        
    }

    @Test
    fun deleteAll_shouldRemoveAllEntities() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = (1..3).map { index ->
            createTestEntity("id$index", "Text $index")
        }

        dao.insertAll(entities)
        assertEquals(3L, dao.getCount())

        dao.deleteAll()
        assertEquals(0L, dao.getCount())
        
        
    }
}