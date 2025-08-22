package me.shadykhalifa.whispertop.data.database.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class UserStatisticsDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: me.shadykhalifa.whispertop.data.database.dao.UserStatisticsDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AppDatabase::class.java
        ).build()
        dao = database.userStatisticsDao()
    }
    
    @After
    fun tearDown() {
        
    }

    private fun createTestEntity(
        id: String = "test-user",
        totalTranscriptions: Long = 0L,
        totalDuration: Float = 0f
    ): UserStatisticsEntity {
        val currentTime = System.currentTimeMillis()
        return UserStatisticsEntity(
            id = id,
            totalWords = 100L,
            totalSessions = 5,
            totalSpeakingTimeMs = 30000L,
            averageWordsPerMinute = 150.0,
            averageWordsPerSession = 20.0,
            userTypingWpm = 40,
            totalTranscriptions = totalTranscriptions,
            totalDuration = totalDuration,
            averageAccuracy = 0.85f,
            dailyUsageCount = 5L,
            mostUsedLanguage = "en",
            mostUsedModel = "whisper-1",
            createdAt = currentTime,
            updatedAt = currentTime
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
        assertEquals(testEntity.totalTranscriptions, retrieved.totalTranscriptions)
        assertEquals(testEntity.averageAccuracy, retrieved.averageAccuracy)
        
        
    }

    @Test
    fun update_shouldModifyExistingEntity() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        val updatedEntity = testEntity.copy(totalTranscriptions = 10L, totalDuration = 150.5f)
        dao.update(updatedEntity)

        val retrieved = dao.getById(testEntity.id)
        assertNotNull(retrieved)
        assertEquals(10L, retrieved.totalTranscriptions)
        assertEquals(150.5f, retrieved.totalDuration)
        
        
    }

    @Test
    fun deleteById_shouldRemoveEntity() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        dao.deleteById(testEntity.id)

        val retrieved = dao.getById(testEntity.id)
        assertNull(retrieved)
        
        
    }

    @Test
    fun getByIdFlow_shouldEmitUpdates() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        
        val flow = dao.getByIdFlow(testEntity.id)
        val initial = flow.first()
        
        assertNotNull(initial)
        assertEquals(testEntity.id, initial.id)
        
        
    }

    @Test
    fun getAllFlow_shouldReturnAllEntities() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("user1"),
            createTestEntity("user2"),
            createTestEntity("user3")
        )

        entities.forEach { dao.insert(it) }
        val retrieved = dao.getAllFlow().first()

        assertEquals(3, retrieved.size)
        
        
    }

    @Test
    fun incrementTranscriptionStats_shouldUpdateCorrectly() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity(totalTranscriptions = 5L, totalDuration = 100f)

        dao.insert(testEntity)
        val newUpdateTime = System.currentTimeMillis()
        dao.incrementTranscriptionStats(
            id = testEntity.id,
            duration = 25.5f,
            dailyUsageCount = 10L,
            updatedAt = newUpdateTime
        )

        val retrieved = dao.getById(testEntity.id)
        assertNotNull(retrieved)
        assertEquals(6L, retrieved.totalTranscriptions) // 5 + 1
        assertEquals(125.5f, retrieved.totalDuration) // 100 + 25.5
        assertEquals(10L, retrieved.dailyUsageCount)
        assertEquals(newUpdateTime, retrieved.updatedAt)
        
        
    }

    @Test
    fun updateDerivedStats_shouldUpdateDerivedFields() = runTest {
        // Use setUp database
        // Use setUp dao
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        val newUpdateTime = System.currentTimeMillis()
        dao.updateDerivedStats(
            id = testEntity.id,
            accuracy = 0.92f,
            language = "es",
            model = "whisper-large",
            updatedAt = newUpdateTime
        )

        val retrieved = dao.getById(testEntity.id)
        assertNotNull(retrieved)
        assertEquals(0.92f, retrieved.averageAccuracy)
        assertEquals("es", retrieved.mostUsedLanguage)
        assertEquals("whisper-large", retrieved.mostUsedModel)
        assertEquals(newUpdateTime, retrieved.updatedAt)
        
        
    }

    @Test
    fun deleteAll_shouldRemoveAllEntities() = runTest {
        // Use setUp database
        // Use setUp dao
        val entities = listOf(
            createTestEntity("user1"),
            createTestEntity("user2"),
            createTestEntity("user3")
        )

        entities.forEach { dao.insert(it) }
        assertEquals(3, dao.getAll().size)

        dao.deleteAll()
        assertEquals(0, dao.getAll().size)
        
        
    }
}