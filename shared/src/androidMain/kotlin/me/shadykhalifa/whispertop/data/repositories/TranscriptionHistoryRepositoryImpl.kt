package me.shadykhalifa.whispertop.data.repositories

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.models.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.models.TranscriptionStatistics
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.utils.Result
import java.util.UUID

class TranscriptionHistoryRepositoryImpl(
    private val dao: TranscriptionHistoryDao
) : BaseRepository(), TranscriptionHistoryRepository {

    override suspend fun saveTranscription(
        text: String,
        duration: Float?,
        audioFilePath: String?,
        confidence: Float?,
        customPrompt: String?,
        temperature: Float?,
        language: String?,
        model: String?
    ): Result<String> = execute {
        val id = UUID.randomUUID().toString()
        val entity = TranscriptionHistoryEntity(
            id = id,
            text = text,
            timestamp = System.currentTimeMillis(),
            duration = duration,
            audioFilePath = audioFilePath,
            confidence = confidence,
            customPrompt = customPrompt,
            temperature = temperature,
            language = language,
            model = model
        )
        dao.insert(entity)
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

    override fun getAllTranscriptions(): Flow<PagingData<TranscriptionHistoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.getAllPaged() }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
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

    override fun searchTranscriptions(query: String): Flow<PagingData<TranscriptionHistoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.searchByText(query) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun getTranscriptionsByDateRange(
        startTime: Long,
        endTime: Long
    ): Flow<PagingData<TranscriptionHistoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.getByDateRange(startTime, endTime) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override fun searchTranscriptionsByTextAndDateRange(
        query: String,
        startTime: Long,
        endTime: Long
    ): Flow<PagingData<TranscriptionHistoryItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { dao.searchByTextAndDateRange(query, startTime, endTime) }
        ).flow.map { pagingData ->
            pagingData.map { it.toDomainModel() }
        }
    }

    override suspend fun getTranscriptionStatistics(): Result<TranscriptionStatistics> = execute {
        val count = dao.getCount()
        val totalDuration = dao.getTotalDuration()
        val averageConfidence = dao.getAverageConfidence()
        val mostUsedLanguage = dao.getMostUsedLanguage()
        val mostUsedModel = dao.getMostUsedModel()
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
            model = model
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
}