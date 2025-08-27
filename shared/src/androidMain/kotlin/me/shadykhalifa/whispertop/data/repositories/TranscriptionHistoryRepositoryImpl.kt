package me.shadykhalifa.whispertop.data.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.data.database.mappers.toDomain
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.SortOption
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistory
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.models.TranscriptionStatistics
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import me.shadykhalifa.whispertop.domain.models.DailyUsage
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.utils.safeCall
import java.util.UUID

class TranscriptionHistoryRepositoryImpl(
    private val dao: TranscriptionHistoryDao,
    private val userStatisticsRepository: me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
) : BaseRepository(), TranscriptionHistoryRepository {

    override suspend fun saveTranscription(
        text: String,
        duration: Float?,
        audioFilePath: String?,
        confidence: Float?,
        customPrompt: String?,
        temperature: Float?,
        language: String?,
        model: String?,
        wordCount: Int
    ): Result<String> = execute {
        val id = UUID.randomUUID().toString()
        val currentTime = System.currentTimeMillis()
        val entity = TranscriptionHistoryEntity(
            id = id,
            text = text,
            timestamp = currentTime,
            duration = duration,
            audioFilePath = audioFilePath,
            confidence = confidence,
            customPrompt = customPrompt,
            temperature = temperature,
            language = language,
            model = model,
            wordCount = wordCount
        )
        dao.insert(entity)

        // Update statistics after saving transcription
        try {
            val userId = "default_user"

            // Update daily aggregated statistics
            val date = kotlinx.datetime.Clock.System.now()
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date

            userStatisticsRepository.updateDailyAggregatedStats(
                date = date,
                totalSessions = 1,
                totalWords = wordCount.toLong(),
                totalSpeakingTime = (duration ?: 0f).toLong() * 1000, // Convert to milliseconds
                averageSessionDuration = (duration ?: 0f).toDouble(),
                peakUsageHour = kotlinx.datetime.Clock.System.now()
                    .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour
            )

            // Update derived statistics (language, model, accuracy)
            userStatisticsRepository.updateDerivedStatistics(userId)

        } catch (e: Exception) {
            // Log error but don't fail the transcription save
            // TODO: Consider implementing proper error reporting/logging
            e.printStackTrace()
        }

        id
    }

    override suspend fun getTranscription(id: String): Result<TranscriptionHistoryItem?> = execute {
        dao.getById(id)?.toDomainModel()
    }

    override suspend fun updateTranscription(transcription: TranscriptionHistoryItem): Result<Unit> = execute {
        dao.update(transcription.toEntity())
    }

    override suspend fun deleteTranscription(id: String): Result<Unit> = execute {
        dao.deleteById(id)
    }

    override suspend fun deleteAllTranscriptions(): Result<Unit> = execute {
        dao.deleteAll()
    }

    override fun getAllTranscriptions(): Flow<List<TranscriptionHistoryItem>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getAllTranscriptionsFlow(): Flow<List<TranscriptionHistoryItem>> {
        return dao.getAllFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getRecentTranscriptions(limit: Int): Result<List<TranscriptionHistoryItem>> = execute {
        dao.getRecent(limit).map { it.toDomainModel() }
    }

    override fun searchTranscriptions(query: String): Flow<List<TranscriptionHistoryItem>> {
        return if (query.isBlank()) {
            // Return limited recent results when no query
            dao.getAllFlow().map { entities ->
                entities.take(1000).map { it.toDomainModel() }
            }
        } else {
            // Use database-level filtering for performance
            dao.searchByTextFlow(query).map { entities ->
                entities.map { it.toDomainModel() }
            }
        }
    }

    override fun getTranscriptionsByDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryItem>> {
        return dao.getByDateRangeFlow(startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun searchTranscriptionsByTextAndDateRange(
        query: String,
        startTime: Long,
        endTime: Long
    ): Flow<List<TranscriptionHistoryItem>> {
        return dao.searchByTextAndDateRangeFlow(query, startTime, endTime).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getTranscriptionStatistics(): Result<TranscriptionStatistics> = execute {
        val count = dao.getCount()
        val totalDuration = dao.getTotalDuration()
        val averageConfidence = dao.getAverageConfidence()
        val mostUsedLanguage = dao.getMostUsedLanguage()?.language
        val mostUsedModel = dao.getMostUsedModel()?.model
        val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % (24 * 60 * 60 * 1000))
        val dailyCount = dao.getDailyTranscriptionCount(startOfDay)

        TranscriptionStatistics(
            totalCount = count,
            totalDuration = totalDuration,
            averageConfidence = averageConfidence,
            mostUsedLanguage = mostUsedLanguage,
            mostUsedModel = mostUsedModel,
            dailyTranscriptionCount = dailyCount
        )
    }

    private fun TranscriptionHistoryEntity.toDomainModel(): TranscriptionHistoryItem {
        val calculatedWordCount = if (wordCount == 0 && text.isNotBlank()) {
            text.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
        } else {
            wordCount
        }
        
        return TranscriptionHistoryItem(
            id = id,
            text = text,
            timestamp = timestamp,
            duration = duration,
            audioFilePath = audioFilePath,
            confidence = confidence,
            customPrompt = customPrompt,
            temperature = temperature,
            language = language,
            model = model,
            wordCount = calculatedWordCount
        )
    }

    private fun TranscriptionHistoryItem.toEntity(): TranscriptionHistoryEntity {
        return TranscriptionHistoryEntity(
            id = id,
            text = text,
            timestamp = timestamp,
            duration = duration,
            audioFilePath = audioFilePath,
            confidence = confidence,
            customPrompt = customPrompt,
            temperature = temperature,
            language = language,
            model = model
        )
    }

    // Enhanced paging methods implementation
    override fun getTranscriptionsPaged(sortOption: SortOption): Flow<PagingData<TranscriptionHistory>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { 
                dao.getAllPagedWithSort(sortOption.toSortString()) 
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun searchTranscriptionsPaged(
        query: String,
        sortOption: SortOption
    ): Flow<PagingData<TranscriptionHistory>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { 
                dao.searchByTextWithSort(query, sortOption.toSortString()) 
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun getTranscriptionsByDateRangePaged(
        dateRange: DateRange,
        sortOption: SortOption
    ): Flow<PagingData<TranscriptionHistory>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { 
                dao.getByDateRangeWithSort(
                    dateRange.startTime,
                    dateRange.endTime,
                    sortOption.toSortString()
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override fun searchTranscriptionsWithFiltersPaged(
        query: String,
        dateRange: DateRange,
        sortOption: SortOption
    ): Flow<PagingData<TranscriptionHistory>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 10,
                enablePlaceholders = true
            ),
            pagingSourceFactory = { 
                dao.searchWithFiltersAndSort(
                    query,
                    dateRange.startTime,
                    dateRange.endTime,
                    sortOption.toSortString()
                )
            }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomain() }
        }
    }

    override suspend fun deleteTranscriptions(ids: List<String>): Result<Int> = execute {
        dao.deleteByIds(ids)
    }

    override suspend fun exportTranscriptions(
        format: ExportFormat,
        dateRange: DateRange
    ): Flow<ExportResult> = flow {
        emit(ExportResult.InProgress(0.0f))
        
        try {
            val totalCount = dao.getExportCount(dateRange.startTime, dateRange.endTime)
            if (totalCount == 0L) {
                emit(ExportResult.Success("empty_export.${format.extension}", 0))
                return@flow
            }
            
            val allTranscriptions = mutableListOf<TranscriptionHistory>()
            val chunkSize = 1000 // Process 1000 items at a time to prevent OOM
            var offset = 0
            
            // Process data in chunks to avoid memory issues
            while (offset < totalCount) {
                val entities = dao.getForExportChunk(
                    dateRange.startTime, 
                    dateRange.endTime, 
                    chunkSize, 
                    offset
                )
                
                if (entities.isEmpty()) break
                
                allTranscriptions.addAll(entities.toDomain())
                offset += entities.size
                
                // Emit progress updates for large exports
                if (totalCount > chunkSize) {
                    val progress = (offset.toFloat() / totalCount * 100).toInt()
                    // Could emit progress updates here if ExportResult supported it
                }
            }
            
            val content = when (format) {
                ExportFormat.JSON -> transcriptionsToJson(allTranscriptions)
                ExportFormat.CSV -> transcriptionsToCsv(allTranscriptions)
                else -> throw IllegalArgumentException("Unsupported format: ${format.name}")
            }
            
            // For now, we'll return the content as a success result
            // In a real implementation, this would write to a file and return file path
            emit(ExportResult.Success("export_${System.currentTimeMillis()}.${format.extension}", allTranscriptions.size))
            
        } catch (e: Exception) {
            emit(ExportResult.Error("Export failed: ${e.message}", e))
        }
    }

    private fun SortOption.toSortString(): String = when (this) {
        is SortOption.DateNewest -> "timestamp_desc"
        is SortOption.DateOldest -> "timestamp_asc"
        is SortOption.DurationLongest -> "duration_desc"
        is SortOption.DurationShortest -> "duration_asc"
        is SortOption.WordCountMost -> "wordCount_desc"
        is SortOption.WordCountLeast -> "wordCount_asc"
        is SortOption.ConfidenceHighest -> "confidence_desc"
        is SortOption.ConfidenceLowest -> "confidence_asc"
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun transcriptionsToJson(transcriptions: List<TranscriptionHistory>): String {
        return try {
            json.encodeToString(transcriptions)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to serialize transcriptions to JSON", e)
        }
    }

    private fun transcriptionsToCsv(transcriptions: List<TranscriptionHistory>): String {
        val headers = "ID,Text,Timestamp,Duration,Language,Model,WordCount,CreatedAt\n"
        val rows = transcriptions.joinToString("\n") { transcription ->
            listOf(
                escapeCsvField(transcription.id),
                escapeCsvField(transcription.text),
                transcription.timestamp.toString(),
                transcription.duration?.toString() ?: "",
                escapeCsvField(transcription.language ?: ""),
                escapeCsvField(transcription.model ?: ""),
                transcription.wordCount.toString(),
                transcription.createdAt.toString()
            ).joinToString(",")
        }
        return headers + rows
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }

    override suspend fun deleteOlderThan(timestamp: Long): Result<Int> = execute {
        dao.deleteOlderThan(timestamp)
    }
    
    override suspend fun getTranscriptionsOlderThan(cutoffTime: Long): Result<List<TranscriptionHistoryItem>> = execute {
        dao.getByTimestampBefore(cutoffTime).map { it.toDomainModel() }
    }
    
    override suspend fun deleteTranscriptionsOlderThan(cutoffTime: Long): Result<Int> = execute {
        dao.deleteOlderThan(cutoffTime)
    }
    
    override suspend fun getRecentTranscriptionSessions(limit: Int): Result<List<TranscriptionSession>> = execute {
        val items = dao.getRecent(limit).map { it.toDomainModel() }
        items.map { it.toTranscriptionSession() }
    }
    
    override suspend fun getDailyUsage(startDate: kotlinx.datetime.LocalDate, endDate: kotlinx.datetime.LocalDate): Result<List<DailyUsage>> = execute {
        val startTimestamp = startDate.toEpochDays() * 24 * 60 * 60 * 1000L
        val endTimestamp = (endDate.toEpochDays() + 1) * 24 * 60 * 60 * 1000L
        
        // Use the existing flow method to get the data
        val entities = dao.getByDateRangeFlow(startTimestamp, endTimestamp).first()
        val items = entities.map { it.toDomainModel() }
        
        // Group by date and calculate daily usage
        val dailyMap = mutableMapOf<kotlinx.datetime.LocalDate, MutableList<TranscriptionHistoryItem>>()
        
        items.forEach { item ->
            val date = kotlinx.datetime.Instant.fromEpochMilliseconds(item.timestamp)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                .date
            
            dailyMap.getOrPut(date) { mutableListOf() }.add(item)
        }
        
        // Convert to DailyUsage objects
        dailyMap.map { (date, dailyItems) ->
            DailyUsage(
                date = date,
                sessionsCount = dailyItems.size,
                wordsTranscribed = dailyItems.sumOf { it.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size.toLong() },
                totalTimeMs = dailyItems.sumOf { ((it.duration ?: 0f) * 1000).toLong() }
            )
        }.sortedBy { it.date }
    }
    
    private fun TranscriptionHistoryItem.toTranscriptionSession(): TranscriptionSession {
        return TranscriptionSession(
            id = this.id,
            timestamp = Instant.fromEpochMilliseconds(this.timestamp),
            audioLengthMs = ((this.duration ?: 0f) * 1000).toLong(),
            wordCount = this.text.split("\\s+".toRegex()).filter { it.isNotBlank() }.size,
            characterCount = this.text.length,
            transcribedText = this.text
        )
    }
    
    override suspend fun getTranscriptionHistory(
        offset: Int,
        limit: Int
    ): Result<List<TranscriptionHistoryItem>> = safeCall {
        dao.getAllForExportChunk(
            startTime = null,
            endTime = null,
            retentionPolicyId = null,
            limit = limit,
            offset = offset
        ).map { it.toDomainModel() }
    }
    
    override suspend fun searchTranscriptionHistory(
        query: String,
        offset: Int,
        limit: Int
    ): Result<List<TranscriptionHistoryItem>> = safeCall {
        // Use a simpler approach - get recent items and filter
        // In a real implementation, this would need a proper search method with pagination
        dao.getRecent(limit * 3) // Get more to account for filtering
            .filter { it.text.contains(query, ignoreCase = true) }
            .drop(offset)
            .take(limit)
            .map { it.toDomainModel() }
    }
    
    override suspend fun getTotalHistoryCount(): Result<Long> = safeCall {
        dao.getTotalCount()
    }
}