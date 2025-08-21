package me.shadykhalifa.whispertop.data.database.dao

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.data.models.UserStatisticsEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UserStatisticsDaoTest {

    private fun createInMemoryDatabase(): AppDatabase {
        return Room.inMemoryDatabaseBuilder(
            klass = AppDatabase::class.java
        ).build()
    }

    private fun createTestEntity(
        id: String = "test-user",
        totalTranscriptions: Long = 0L,
        totalDuration: Float = 0f
    ): UserStatisticsEntity {
        val currentTime = System.currentTimeMillis()
        return UserStatisticsEntity(
            id = id,
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
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        val retrieved = dao.getById(testEntity.id)

        assertNotNull(retrieved)
        assertEquals(testEntity.id, retrieved.id)
        assertEquals(testEntity.totalTranscriptions, retrieved.totalTranscriptions)
        assertEquals(testEntity.averageAccuracy, retrieved.averageAccuracy)
        
        database.close()
    }

    @Test
    fun update_shouldModifyExistingEntity() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        val updatedEntity = testEntity.copy(totalTranscriptions = 10L, totalDuration = 150.5f)
        dao.update(updatedEntity)

        val retrieved = dao.getById(testEntity.id)
        assertNotNull(retrieved)
        assertEquals(10L, retrieved.totalTranscriptions)
        assertEquals(150.5f, retrieved.totalDuration)
        
        database.close()
    }

    @Test
    fun deleteById_shouldRemoveEntity() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        dao.deleteById(testEntity.id)

        val retrieved = dao.getById(testEntity.id)
        assertNull(retrieved)
        
        database.close()
    }

    @Test
    fun getByIdFlow_shouldEmitUpdates() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
        val testEntity = createTestEntity()

        dao.insert(testEntity)
        
        val flow = dao.getByIdFlow(testEntity.id)
        val initial = flow.first()
        
        assertNotNull(initial)
        assertEquals(testEntity.id, initial.id)
        
        database.close()
    }

    @Test
    fun getAllFlow_shouldReturnAllEntities() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
        val entities = listOf(
            createTestEntity("user1"),
            createTestEntity("user2"),
            createTestEntity("user3")
        )

        entities.forEach { dao.insert(it) }
        val retrieved = dao.getAllFlow().first()

        assertEquals(3, retrieved.size)
        
        database.close()
    }

    @Test
    fun incrementTranscriptionStats_shouldUpdateCorrectly() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
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
        
        database.close()
    }

    @Test
    fun updateDerivedStats_shouldUpdateDerivedFields() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
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
        
        database.close()
    }

    @Test
    fun deleteAll_shouldRemoveAllEntities() = runTest {
        val database = createInMemoryDatabase()
        val dao = database.userStatisticsDao()
        val entities = listOf(
            createTestEntity("user1"),
            createTestEntity("user2"),
            createTestEntity("user3")
        )

        entities.forEach { dao.insert(it) }
        assertEquals(3, dao.getAll().size)

        dao.deleteAll()
        assertEquals(0, dao.getAll().size)
        
        database.close()
    }
}