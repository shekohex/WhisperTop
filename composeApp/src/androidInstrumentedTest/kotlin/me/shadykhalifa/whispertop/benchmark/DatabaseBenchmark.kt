package me.shadykhalifa.whispertop.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.data.database.entities.UserStatisticsEntity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class DatabaseBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    private lateinit var database: AppDatabase
    private val random = Random(42) // Fixed seed for reproducible benchmarks

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun benchmarkSingleInsert() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            // Baseline: Single insert should complete < 5ms
            val startTime = System.nanoTime()
            runBlocking {
                val entity = generateTranscriptionEntity()
                dao.insert(entity)
                dao.deleteById(entity.id) // Clean up for next iteration
            }
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            assert(duration < 5) { "Single insert took ${duration}ms, exceeds 5ms baseline" }
        }
    }

    @Test
    fun benchmarkBatchInsert_small() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            // Baseline: Small batch (10 items) should complete < 20ms
            val startTime = System.nanoTime()
            runBlocking {
                val entities = generateTranscriptionEntities(10)
                dao.insertAll(entities)
                dao.deleteAll() // Clean up for next iteration
            }
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            assert(duration < 20) { "Small batch insert took ${duration}ms, exceeds 20ms baseline" }
        }
    }

    @Test
    fun benchmarkBatchInsert_medium() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                val entities = generateTranscriptionEntities(100)
                dao.insertAll(entities)
                dao.deleteAll() // Clean up for next iteration
            }
        }
    }

    @Test
    fun benchmarkBatchInsert_large() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            // Baseline: Large batch (1000 items) should complete < 500ms
            val startTime = System.nanoTime()
            runBlocking {
                val entities = generateTranscriptionEntities(1000)
                dao.insertAll(entities)
                dao.deleteAll() // Clean up for next iteration
            }
            val duration = (System.nanoTime() - startTime) / 1_000_000
            
            assert(duration < 500) { "Large batch insert took ${duration}ms, exceeds 500ms baseline" }
        }
    }

    @Test
    fun benchmarkQueryById_withIndex() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert test data
        val testEntities = generateTranscriptionEntities(1000)
        dao.insertAll(testEntities)
        val targetId = testEntities[500].id
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.getById(targetId)
            }
        }
    }

    @Test
    fun benchmarkTextSearch_smallDataset() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert test data with searchable text
        val testEntities = generateTranscriptionEntitiesWithText(100)
        dao.insertAll(testEntities)
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.searchByTextFlow("test").first()
            }
        }
    }

    @Test
    fun benchmarkTextSearch_largeDataset() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert large dataset
        val testEntities = generateTranscriptionEntitiesWithText(5000)
        dao.insertAll(testEntities)
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.searchByTextFlow("test").first()
            }
        }
    }

    @Test
    fun benchmarkDateRangeQuery() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert data across different time ranges
        val now = Clock.System.now().toEpochMilliseconds()
        val entities = generateTranscriptionEntitiesWithTimestamps(1000, now)
        dao.insertAll(entities)
        
        val startTime = now - 86400000L // 24 hours ago
        val endTime = now
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.getByDateRangeFlow(startTime, endTime).first()
            }
        }
    }

    @Test
    fun benchmarkComplexQuery_multipleFilters() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert diverse data
        val now = Clock.System.now().toEpochMilliseconds()
        val entities = generateComplexTranscriptionEntities(1000, now)
        dao.insertAll(entities)
        
        val startTime = now - 86400000L
        val endTime = now
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.searchWithFiltersAndSort(
                    query = "test",
                    startTime = startTime,
                    endTime = endTime,
                    sortBy = "timestamp_desc"
                ).load(
                    androidx.paging.PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = 20,
                        placeholdersEnabled = false
                    )
                )
            }
        }
    }

    @Test
    fun benchmarkAggregateQueries() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert data with various durations and confidences
        val entities = generateTranscriptionEntitiesWithMetrics(1000)
        dao.insertAll(entities)
        
        benchmarkRule.measureRepeated {
            runBlocking {
                // Run multiple aggregate queries
                dao.getCount()
                dao.getTotalDuration()
                dao.getAverageConfidence()
                dao.getMostUsedLanguage()
                dao.getMostUsedModel()
            }
        }
    }

    @Test
    fun benchmarkUpdate_singleRecord() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert a record to update
        val entity = generateTranscriptionEntity()
        dao.insert(entity)
        
        benchmarkRule.measureRepeated {
            runBlocking {
                val updated = entity.copy(
                    text = "Updated text ${random.nextInt()}",
                    confidence = random.nextFloat()
                )
                dao.update(updated)
            }
        }
    }

    @Test
    fun benchmarkBulkDelete() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                // Setup: Insert data to delete
                val entities = generateTranscriptionEntities(100)
                dao.insertAll(entities)
                
                // Delete half of them
                val idsToDelete = entities.take(50).map { it.id }
                dao.deleteByIds(idsToDelete)
                
                // Clean up remaining
                dao.deleteAll()
            }
        }
    }

    @Test
    fun benchmarkPagingSource_load() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert large dataset for paging
        val entities = generateTranscriptionEntities(2000)
        dao.insertAll(entities)
        
        val pagingSource = dao.getAllPaged()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                pagingSource.load(
                    androidx.paging.PagingSource.LoadParams.Refresh(
                        key = null,
                        loadSize = 50,
                        placeholdersEnabled = false
                    )
                )
            }
        }
    }

    @Test
    fun benchmarkExportQuery_chunked() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        // Setup: Insert large dataset
        val entities = generateTranscriptionEntities(5000)
        dao.insertAll(entities)
        
        benchmarkRule.measureRepeated {
            runBlocking {
                dao.getForExportChunk(
                    startTime = null,
                    endTime = null,
                    limit = 500,
                    offset = 1000
                )
            }
        }
    }

    @Test
    fun benchmarkUserStatistics_operations() = runBlocking {
        val dao = database.userStatisticsDao()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                // Insert stats
                val stats = UserStatisticsEntity(
                    id = "perf_test_${random.nextInt()}",
                    totalWords = random.nextLong(10000),
                    totalSessions = random.nextInt(1000),
                    totalTranscriptions = random.nextLong(5000)
                )
                dao.insert(stats)
                
                // Update stats
                dao.incrementTranscriptionStats(
                    id = stats.id,
                    duration = random.nextFloat() * 60,
                    dailyUsageCount = random.nextLong(100),
                    updatedAt = Clock.System.now().toEpochMilliseconds()
                )
                
                // Read stats
                dao.getById(stats.id)
                
                // Clean up
                dao.deleteById(stats.id)
            }
        }
    }

    @Test
    fun benchmarkConcurrentAccess() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                // Simulate concurrent read/write operations
                val jobs = List(10) { index ->
                    kotlinx.coroutines.async {
                        val entity = generateTranscriptionEntity().copy(id = "concurrent_$index")
                        dao.insert(entity)
                        dao.getById(entity.id)
                        dao.deleteById(entity.id)
                    }
                }
                jobs.forEach { it.await() }
            }
        }
    }

    @Test
    fun benchmarkTransactionPerformance() = runBlocking {
        val dao = database.transcriptionHistoryDao()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                database.runInTransaction {
                    val entities = generateTranscriptionEntities(50)
                    entities.forEach { entity ->
                        dao.insert(entity)
                    }
                    // Update some entities
                    entities.take(10).forEach { entity ->
                        dao.update(entity.copy(text = "Updated in transaction"))
                    }
                    // Delete some entities
                    entities.takeLast(10).forEach { entity ->
                        dao.delete(entity)
                    }
                }
                dao.deleteAll() // Clean up
            }
        }
    }

    // Helper functions for generating test data

    private fun generateTranscriptionEntity(): TranscriptionHistoryEntity {
        val now = Clock.System.now().toEpochMilliseconds()
        return TranscriptionHistoryEntity(
            id = "bench_${random.nextInt()}",
            text = "Benchmark transcription text ${random.nextInt()}",
            timestamp = now - random.nextLong(86400000L), // Within last 24 hours
            duration = random.nextFloat() * 30f,
            confidence = 0.8f + random.nextFloat() * 0.2f,
            language = listOf("en", "es", "fr", "de").random(),
            model = listOf("whisper-1", "whisper-2").random(),
            wordCount = random.nextInt(1, 100)
        )
    }

    private fun generateTranscriptionEntities(count: Int): List<TranscriptionHistoryEntity> {
        return (0 until count).map { generateTranscriptionEntity() }
    }

    private fun generateTranscriptionEntitiesWithText(count: Int): List<TranscriptionHistoryEntity> {
        val textVariations = listOf(
            "This is a test transcription for benchmarking",
            "Performance test audio transcription",
            "Benchmark test with searchable content",
            "Audio processing test result",
            "Speech to text conversion benchmark"
        )
        
        return (0 until count).map { index ->
            generateTranscriptionEntity().copy(
                id = "text_bench_$index",
                text = textVariations[index % textVariations.size] + " $index"
            )
        }
    }

    private fun generateTranscriptionEntitiesWithTimestamps(count: Int, baseTime: Long): List<TranscriptionHistoryEntity> {
        return (0 until count).map { index ->
            generateTranscriptionEntity().copy(
                id = "time_bench_$index",
                timestamp = baseTime - (index * 3600000L) // Spread across hours
            )
        }
    }

    private fun generateComplexTranscriptionEntities(count: Int, baseTime: Long): List<TranscriptionHistoryEntity> {
        return (0 until count).map { index ->
            TranscriptionHistoryEntity(
                id = "complex_bench_$index",
                text = "Complex benchmark test transcription $index with searchable content",
                timestamp = baseTime - random.nextLong(86400000L * 7), // Within last week
                duration = random.nextFloat() * 120f,
                confidence = 0.5f + random.nextFloat() * 0.5f,
                language = listOf("en", "es", "fr", "de", "it", "pt").random(),
                model = listOf("whisper-1", "whisper-2", "whisper-large").random(),
                wordCount = random.nextInt(1, 200),
                customPrompt = if (random.nextBoolean()) "Custom prompt $index" else null,
                temperature = random.nextFloat()
            )
        }
    }

    private fun generateTranscriptionEntitiesWithMetrics(count: Int): List<TranscriptionHistoryEntity> {
        return (0 until count).map { index ->
            generateTranscriptionEntity().copy(
                id = "metrics_bench_$index",
                duration = random.nextFloat() * 180f, // 0-3 minutes
                confidence = random.nextFloat(), // Full range
                wordCount = random.nextInt(1, 300)
            )
        }
    }
}