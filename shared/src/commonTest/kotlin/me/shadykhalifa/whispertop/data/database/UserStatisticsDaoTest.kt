package me.shadykhalifa.whispertop.data.database

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserStatisticsDaoTest {
    
    private lateinit var database: AppDatabase
    private lateinit var dao: me.shadykhalifa.whispertop.data.database.dao.UserStatisticsDao

    @BeforeTest
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder<AppDatabase>()
            .build()
        dao = database.userStatisticsDao()
    }

    @AfterTest
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveStatistics() = runTest {
        val stats = createTestStatistics()
        
        dao.insert(stats)
        val retrieved = dao.getById()
        
        assertNotNull(retrieved)
        assertEquals(stats.totalTranscriptions, retrieved.totalTranscriptions)
        assertEquals(stats.dailyUsageCount, retrieved.dailyUsageCount)
    }

    @Test
    fun updateStatistics() = runTest {
        val stats = createTestStatistics()
        dao.insert(stats)
        
        val updated = stats.copy(dailyUsageCount = 10)
        dao.update(updated)
        
        val retrieved = dao.getById()
        assertNotNull(retrieved)
        assertEquals(10, retrieved.dailyUsageCount)
    }

    @Test
    fun incrementUsage() = runTest {
        val stats = createTestStatistics()
        dao.insert(stats)
        
        dao.incrementUsage(
            durationMinutes = 2.5f,
            wordCount = 50,
            currentDate = "2024-01-01"
        )
        
        val retrieved = dao.getById()
        assertNotNull(retrieved)
        assertEquals(1, retrieved.totalTranscriptions)
        assertEquals(2.5f, retrieved.totalDurationMinutes)
        assertEquals(50, retrieved.totalWordsTranscribed)
        assertEquals(1, retrieved.dailyUsageCount)
        assertEquals("2024-01-01", retrieved.lastUsedDate)
    }

    @Test
    fun updateMostUsedLanguage() = runTest {
        val stats = createTestStatistics()
        dao.insert(stats)
        
        dao.updateMostUsedLanguage("Spanish")
        
        val retrieved = dao.getById()
        assertNotNull(retrieved)
        assertEquals("Spanish", retrieved.mostUsedLanguage)
    }

    @Test
    fun updateMostUsedModel() = runTest {
        val stats = createTestStatistics()
        dao.insert(stats)
        
        dao.updateMostUsedModel("whisper-large")
        
        val retrieved = dao.getById()
        assertNotNull(retrieved)
        assertEquals("whisper-large", retrieved.mostUsedModel)
    }

    @Test
    fun updateAverageConfidence() = runTest {
        val stats = createTestStatistics()
        dao.insert(stats)
        
        dao.updateAverageConfidence(0.95f)
        
        val retrieved = dao.getById()
        assertNotNull(retrieved)
        assertEquals(0.95f, retrieved.averageConfidence)
    }

    @Test
    fun resetDailyCount() = runTest {
        val stats = createTestStatistics(dailyUsageCount = 10)
        dao.insert(stats)
        
        dao.resetDailyCount()
        
        val retrieved = dao.getById()
        assertNotNull(retrieved)
        assertEquals(0, retrieved.dailyUsageCount)
    }

    @Test
    fun flowUpdates() = runTest {
        val stats = createTestStatistics()
        
        dao.insert(stats)
        val flow = dao.getByIdFlow()
        val result = flow.first()
        
        assertNotNull(result)
        assertEquals(stats.totalTranscriptions, result.totalTranscriptions)
    }

    @Test
    fun deleteAll() = runTest {
        val stats = createTestStatistics()
        dao.insert(stats)
        
        dao.deleteAll()
        val retrieved = dao.getById()
        
        assertNull(retrieved)
    }

    private fun createTestStatistics(
        totalTranscriptions: Long = 0,
        dailyUsageCount: Int = 0
    ) = UserStatisticsEntity(
        totalTranscriptions = totalTranscriptions,
        dailyUsageCount = dailyUsageCount,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}