package me.shadykhalifa.whispertop.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
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
    private lateinit var dao: UserStatisticsDao

    private val testStats = UserStatisticsEntity(
        id = "test_user_stats",
        totalWords = 1500L,
        totalSessions = 25,
        totalSpeakingTimeMs = 120000L, // 2 minutes
        averageWordsPerMinute = 45.0,
        averageWordsPerSession = 60.0,
        userTypingWpm = 65,
        totalTranscriptions = 30L,
        totalDuration = 180.5f,
        averageAccuracy = 0.92f,
        dailyUsageCount = 5L,
        mostUsedLanguage = "en",
        mostUsedModel = "whisper-1"
    )

    private val defaultStats = UserStatisticsEntity()

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        dao = database.userStatisticsDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertAndGetById_success() = runTest {
        dao.insert(testStats)
        
        val retrieved = dao.getById(testStats.id)
        
        assertNotNull(retrieved)
        assertEquals(testStats.id, retrieved.id)
        assertEquals(testStats.totalWords, retrieved.totalWords)
        assertEquals(testStats.totalSessions, retrieved.totalSessions)
        assertEquals(testStats.averageWordsPerMinute, retrieved.averageWordsPerMinute)
        assertEquals(testStats.mostUsedLanguage, retrieved.mostUsedLanguage)
    }

    @Test
    fun insert_withDefaultValues() = runTest {
        dao.insert(defaultStats)
        
        val retrieved = dao.getById(defaultStats.id)
        
        assertNotNull(retrieved)
        assertEquals("user_stats", retrieved.id)
        assertEquals(0L, retrieved.totalWords)
        assertEquals(0, retrieved.totalSessions)
        assertEquals(0.0, retrieved.averageWordsPerMinute)
        assertNull(retrieved.averageAccuracy)
        assertNull(retrieved.mostUsedLanguage)
        assertNull(retrieved.mostUsedModel)
    }

    @Test
    fun update_success() = runTest {
        dao.insert(testStats)
        
        val updated = testStats.copy(
            totalWords = 2000L,
            totalSessions = 30,
            averageWordsPerMinute = 50.0
        )
        dao.update(updated)
        
        val retrieved = dao.getById(testStats.id)
        assertNotNull(retrieved)
        assertEquals(2000L, retrieved.totalWords)
        assertEquals(30, retrieved.totalSessions)
        assertEquals(50.0, retrieved.averageWordsPerMinute)
    }

    @Test
    fun getByIdFlow_reactsToChanges() = runTest {
        // Initially should be null
        val initialFlow = dao.getByIdFlow(testStats.id)
        assertNull(initialFlow.first())
        
        // Insert data
        dao.insert(testStats)
        
        // Should now return the inserted data
        val afterInsert = dao.getByIdFlow(testStats.id).first()
        assertNotNull(afterInsert)
        assertEquals(testStats.id, afterInsert.id)
        
        // Update data
        val updatedStats = testStats.copy(totalWords = 3000L)
        dao.update(updatedStats)
        
        // Flow should reflect the update
        val afterUpdate = dao.getByIdFlow(testStats.id).first()
        assertNotNull(afterUpdate)
        assertEquals(3000L, afterUpdate.totalWords)
    }

    @Test
    fun getAll_returnsAllStatistics() = runTest {
        val stats1 = testStats.copy(id = "user1")
        val stats2 = testStats.copy(id = "user2", totalWords = 2500L)
        
        dao.insert(stats1)
        dao.insert(stats2)
        
        val allStats = dao.getAll()
        
        assertEquals(2, allStats.size)
        // Should be ordered by updatedAt DESC
        val ids = allStats.map { it.id }
        assertEquals(setOf("user1", "user2"), ids.toSet())
    }

    @Test
    fun getAllFlow_reactsToChanges() = runTest {
        // Initially empty
        assertEquals(0, dao.getAllFlow().first().size)
        
        // Insert data
        dao.insert(testStats)
        assertEquals(1, dao.getAllFlow().first().size)
        
        // Insert more data
        val anotherStats = testStats.copy(id = "another_user")
        dao.insert(anotherStats)
        assertEquals(2, dao.getAllFlow().first().size)
    }

    @Test
    fun deleteById_success() = runTest {
        dao.insert(testStats)
        
        dao.deleteById(testStats.id)
        
        val retrieved = dao.getById(testStats.id)
        assertNull(retrieved)
    }

    @Test
    fun deleteAll_success() = runTest {
        dao.insert(testStats)
        dao.insert(testStats.copy(id = "another_user"))
        
        dao.deleteAll()
        
        val allStats = dao.getAll()
        assertEquals(0, allStats.size)
    }

    @Test
    fun incrementTranscriptionStats_updatesCorrectly() = runTest {
        dao.insert(testStats)
        
        val newUpdatedAt = Clock.System.now().toEpochMilliseconds()
        dao.incrementTranscriptionStats(
            id = testStats.id,
            duration = 15.5f,
            dailyUsageCount = 6L,
            updatedAt = newUpdatedAt
        )
        
        val updated = dao.getById(testStats.id)
        assertNotNull(updated)
        assertEquals(31L, updated.totalTranscriptions) // 30 + 1
        assertEquals(196.0f, updated.totalDuration, 0.01f) // 180.5 + 15.5
        assertEquals(6L, updated.dailyUsageCount)
        assertEquals(newUpdatedAt, updated.updatedAt)
    }

    @Test
    fun updateDerivedStats_updatesCorrectly() = runTest {
        dao.insert(testStats)
        
        val newUpdatedAt = Clock.System.now().toEpochMilliseconds()
        dao.updateDerivedStats(
            id = testStats.id,
            accuracy = 0.95f,
            language = "es",
            model = "whisper-2",
            updatedAt = newUpdatedAt
        )
        
        val updated = dao.getById(testStats.id)
        assertNotNull(updated)
        assertEquals(0.95f, updated.averageAccuracy)
        assertEquals("es", updated.mostUsedLanguage)
        assertEquals("whisper-2", updated.mostUsedModel)
        assertEquals(newUpdatedAt, updated.updatedAt)
    }

    @Test
    fun updateDerivedStats_withNullValues() = runTest {
        dao.insert(testStats)
        
        val newUpdatedAt = Clock.System.now().toEpochMilliseconds()
        dao.updateDerivedStats(
            id = testStats.id,
            accuracy = null,
            language = null,
            model = null,
            updatedAt = newUpdatedAt
        )
        
        val updated = dao.getById(testStats.id)
        assertNotNull(updated)
        assertNull(updated.averageAccuracy)
        assertNull(updated.mostUsedLanguage)
        assertNull(updated.mostUsedModel)
        assertEquals(newUpdatedAt, updated.updatedAt)
    }

    @Test
    fun insert_withConflictStrategy() = runTest {
        // Insert original
        dao.insert(testStats)
        
        // Insert with same ID (should replace due to OnConflictStrategy.REPLACE)
        val updated = testStats.copy(
            totalWords = 5000L,
            mostUsedLanguage = "fr"
        )
        dao.insert(updated)
        
        val retrieved = dao.getById(testStats.id)
        assertNotNull(retrieved)
        assertEquals(5000L, retrieved.totalWords)
        assertEquals("fr", retrieved.mostUsedLanguage)
        
        // Should still have only 1 record for this ID
        val allStats = dao.getAll()
        assertEquals(1, allStats.size)
    }

    @Test
    fun statisticsCalculations_precision() = runTest {
        val precisionStats = UserStatisticsEntity(
            id = "precision_test",
            totalWords = 12345L,
            totalSessions = 123,
            totalSpeakingTimeMs = 987654321L,
            averageWordsPerMinute = 123.456789,
            averageWordsPerSession = 100.365853,
            totalTranscriptions = 9876L,
            totalDuration = 123456.789f,
            averageAccuracy = 0.98765f
        )
        
        dao.insert(precisionStats)
        
        val retrieved = dao.getById(precisionStats.id)
        assertNotNull(retrieved)
        
        // Verify precision is maintained
        assertEquals(123.456789, retrieved.averageWordsPerMinute, 0.000001)
        assertEquals(100.365853, retrieved.averageWordsPerSession, 0.000001)
        assertEquals(0.98765f, retrieved.averageAccuracy, 0.00001f)
        assertEquals(123456.789f, retrieved.totalDuration, 0.001f)
    }

    @Test
    fun timestampHandling_createdAndUpdated() = runTest {
        val beforeInsert = Clock.System.now().toEpochMilliseconds()
        
        dao.insert(testStats)
        
        val retrieved = dao.getById(testStats.id)
        assertNotNull(retrieved)
        
        // createdAt and updatedAt should be set during insert
        assert(retrieved.createdAt >= beforeInsert)
        assert(retrieved.updatedAt >= beforeInsert)
        
        val originalUpdatedAt = retrieved.updatedAt
        
        // Wait a small amount and update
        Thread.sleep(10)
        
        val updated = retrieved.copy(
            totalWords = retrieved.totalWords + 100,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        dao.update(updated)
        
        val afterUpdate = dao.getById(testStats.id)
        assertNotNull(afterUpdate)
        
        // createdAt should remain the same, updatedAt should be newer
        assertEquals(retrieved.createdAt, afterUpdate.createdAt)
        assert(afterUpdate.updatedAt > originalUpdatedAt)
    }

    @Test
    fun multipleUsers_independentStats() = runTest {
        val user1Stats = testStats.copy(
            id = "user1",
            totalWords = 1000L,
            mostUsedLanguage = "en"
        )
        val user2Stats = testStats.copy(
            id = "user2",
            totalWords = 2000L,
            mostUsedLanguage = "es"
        )
        
        dao.insert(user1Stats)
        dao.insert(user2Stats)
        
        // Update user1 stats
        dao.incrementTranscriptionStats(
            id = "user1",
            duration = 10f,
            dailyUsageCount = 1L,
            updatedAt = Clock.System.now().toEpochMilliseconds()
        )
        
        // Verify user2 stats are unchanged
        val user1Updated = dao.getById("user1")
        val user2Unchanged = dao.getById("user2")
        
        assertNotNull(user1Updated)
        assertNotNull(user2Unchanged)
        
        assertEquals(31L, user1Updated.totalTranscriptions) // incremented
        assertEquals(30L, user2Unchanged.totalTranscriptions) // unchanged
        
        assertEquals(1000L, user1Updated.totalWords)
        assertEquals(2000L, user2Unchanged.totalWords)
    }
}