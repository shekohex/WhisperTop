package me.shadykhalifa.whispertop.data.database

import androidx.paging.PagingSource
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
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
class TranscriptionHistoryDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: TranscriptionHistoryDao

    private val testEntity = TranscriptionHistoryEntity(
        id = "test_integration_1",
        text = "This is a comprehensive integration test for the transcription history DAO",
        timestamp = Clock.System.now().toEpochMilliseconds(),
        duration = 15.5f,
        audioFilePath = "/test/path/integration_audio.wav",
        confidence = 0.97f,
        customPrompt = "Integration test prompt",
        temperature = 0.6f,
        language = "en",
        model = "whisper-1",
        wordCount = 12
    )

    private val testEntity2 = TranscriptionHistoryEntity(
        id = "test_integration_2",
        text = "Segunda prueba para verificar funcionalidad multiidioma",
        timestamp = Clock.System.now().toEpochMilliseconds() - 1800000, // 30 minutes ago
        duration = 8.2f,
        audioFilePath = "/test/path/spanish_audio.wav",
        confidence = 0.91f,
        customPrompt = null,
        temperature = 0.7f,
        language = "es",
        model = "whisper-1", 
        wordCount = 7
    )

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        
        dao = database.transcriptionHistoryDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun basicCrudOperations_workCorrectly() = runTest {
        // Test Insert
        dao.insert(testEntity)
        
        // Test GetById
        val retrieved = dao.getById(testEntity.id)
        assertNotNull(retrieved)
        assertEquals(testEntity.id, retrieved.id)
        assertEquals(testEntity.text, retrieved.text)
        assertEquals(testEntity.confidence, retrieved.confidence)
        
        // Test Update
        val updated = testEntity.copy(
            text = "Updated integration test text",
            confidence = 0.99f
        )
        dao.update(updated)
        
        val updatedRetrieved = dao.getById(testEntity.id)
        assertNotNull(updatedRetrieved)
        assertEquals("Updated integration test text", updatedRetrieved.text)
        assertEquals(0.99f, updatedRetrieved.confidence)
        
        // Test Delete
        dao.delete(updated)
        val afterDelete = dao.getById(testEntity.id)
        assertNull(afterDelete)
    }

    @Test
    fun batchOperations_workCorrectly() = runTest {
        // Test insertAll
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val count = dao.getCount()
        assertEquals(2L, count)
        
        // Test deleteAll
        dao.deleteAll()
        val countAfterDelete = dao.getCount()
        assertEquals(0L, countAfterDelete)
    }

    @Test
    fun flowBasedQueries_returnCorrectData() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        // Test getAllFlow
        val allTranscriptions = dao.getAllFlow().first()
        assertEquals(2, allTranscriptions.size)
        
        // Should be ordered by timestamp DESC (most recent first)
        assertTrue(allTranscriptions[0].timestamp >= allTranscriptions[1].timestamp)
        assertEquals(testEntity.id, allTranscriptions[0].id)
    }

    @Test
    fun searchOperations_findCorrectResults() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        // Search using flow-based method
        val searchResults = dao.searchByTextFlow("integration").first()
        assertEquals(1, searchResults.size)
        assertEquals(testEntity.id, searchResults[0].id)
        
        // Case insensitive search
        val caseInsensitiveResults = dao.searchByTextFlow("INTEGRATION").first()
        assertEquals(1, caseInsensitiveResults.size)
        
        // Search in Spanish
        val spanishResults = dao.searchByTextFlow("prueba").first()
        assertEquals(1, spanishResults.size)
        assertEquals(testEntity2.id, spanishResults[0].id)
    }

    @Test
    fun dateRangeQueries_filterCorrectly() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val now = Clock.System.now().toEpochMilliseconds()
        val fifteenMinutesAgo = now - 900000
        
        // Get recent transcriptions (last 15 minutes)
        val recentResults = dao.getByDateRangeFlow(fifteenMinutesAgo, now).first()
        assertEquals(1, recentResults.size)
        assertEquals(testEntity.id, recentResults[0].id)
    }

    @Test
    fun statisticsQueries_calculateCorrectly() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val totalDuration = dao.getTotalDuration()
        val avgConfidence = dao.getAverageConfidence()
        
        assertNotNull(totalDuration)
        assertEquals(23.7f, totalDuration, 0.01f) // 15.5 + 8.2
        
        assertNotNull(avgConfidence)
        assertEquals(0.94f, avgConfidence, 0.01f) // (0.97 + 0.91) / 2
    }

    @Test
    fun pagingSourceOperations_workCorrectly() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val pagingSource = dao.getAllPaged()
        
        // Test loading first page
        val loadResult = pagingSource.load(
            PagingSource.LoadParams.Refresh(
                key = null,
                loadSize = 10,
                placeholdersEnabled = false
            )
        )
        
        assertTrue(loadResult is PagingSource.LoadResult.Page)
        val page = loadResult as PagingSource.LoadResult.Page
        assertEquals(2, page.data.size)
        
        // Should be ordered by timestamp DESC
        assertTrue(page.data[0].timestamp >= page.data[1].timestamp)
    }

    @Test
    fun bulkDeleteOperations_workCorrectly() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        // Test deleteByIds
        val deletedCount = dao.deleteByIds(listOf(testEntity.id))
        assertEquals(1, deletedCount)
        
        // Verify only one remains
        val remainingCount = dao.getCount()
        assertEquals(1L, remainingCount)
        
        val remaining = dao.getById(testEntity2.id)
        assertNotNull(remaining)
        assertEquals(testEntity2.id, remaining.id)
    }

    @Test
    fun exportQueries_returnCorrectData() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val now = Clock.System.now().toEpochMilliseconds()
        val twoHoursAgo = now - 7200000
        
        val exportCount = dao.getExportCount(twoHoursAgo, now)
        assertEquals(2L, exportCount)
        
        val exportData = dao.getForExportChunk(twoHoursAgo, now, 10, 0)
        assertEquals(2, exportData.size)
        
        // Should be ordered by timestamp DESC
        assertTrue(exportData[0].timestamp >= exportData[1].timestamp)
    }

    @Test
    fun recentTranscriptions_returnCorrectLimit() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val recent = dao.getRecent(1)
        assertEquals(1, recent.size)
        assertEquals(testEntity.id, recent[0].id) // Most recent
        
        val allRecent = dao.getRecent(5)
        assertEquals(2, allRecent.size) // Only 2 exist
    }

    @Test
    fun advancedSearchCombinations_workCorrectly() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        val now = Clock.System.now().toEpochMilliseconds()
        val oneHourAgo = now - 3600000
        
        // Search with text and date range
        val combinedResults = dao.searchByTextAndDateRangeFlow(
            "integration", oneHourAgo, now
        ).first()
        
        assertEquals(1, combinedResults.size)
        assertEquals(testEntity.id, combinedResults[0].id)
    }

    @Test
    fun sortingWithPaging_worksCorrectly() = runTest {
        dao.insertAll(listOf(testEntity, testEntity2))
        
        // Test different sort orders
        val sortByTimestampDesc = dao.getAllPagedWithSort("timestamp_desc")
        val sortByDurationAsc = dao.getAllPagedWithSort("duration_asc")
        
        // Load from timestamp desc paging source
        val timestampResult = sortByTimestampDesc.load(
            PagingSource.LoadParams.Refresh(null, 10, false)
        )
        
        assertTrue(timestampResult is PagingSource.LoadResult.Page)
        val timestampPage = timestampResult as PagingSource.LoadResult.Page
        assertEquals(2, timestampPage.data.size)
        assertEquals(testEntity.id, timestampPage.data[0].id) // Most recent first
        
        // Load from duration asc paging source
        val durationResult = sortByDurationAsc.load(
            PagingSource.LoadParams.Refresh(null, 10, false)
        )
        
        assertTrue(durationResult is PagingSource.LoadResult.Page)
        val durationPage = durationResult as PagingSource.LoadResult.Page
        assertEquals(2, durationPage.data.size)
        assertEquals(testEntity2.id, durationPage.data[0].id) // Shorter duration first (8.2 < 15.5)
    }
}