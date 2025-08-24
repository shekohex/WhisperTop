package me.shadykhalifa.whispertop.data.repositories

import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.database.entities.TranscriptionHistoryEntity
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionDatabaseRepository
import me.shadykhalifa.whispertop.utils.Result

class TranscriptionDatabaseRepositoryImpl(
    private val dao: TranscriptionHistoryDao
) : BaseRepository(), TranscriptionDatabaseRepository {

    override suspend fun getAllForExport(
        dateRange: DateRange,
        retentionPolicyId: String?,
        limit: Int,
        offset: Int
    ): Result<List<TranscriptionHistoryItem>> = execute {
        val entities = dao.getAllForExportChunk(
            startTime = dateRange.startTime,
            endTime = dateRange.endTime,
            retentionPolicyId = retentionPolicyId,
            limit = limit,
            offset = offset
        )
        entities.map { it.toDomainModel() }
    }

    override suspend fun markAsExported(ids: List<String>): Result<Int> = execute {
        val currentTime = Clock.System.now().toEpochMilliseconds()
        dao.markAsExported(ids, currentTime)
    }

    override suspend fun getByRetentionPolicy(policyId: String): Result<List<TranscriptionHistoryItem>> = execute {
        dao.getByRetentionPolicy(policyId).map { it.toDomainModel() }
    }

    override suspend fun getExpiredByRetentionPolicy(
        policyId: String,
        cutoffTime: Long
    ): Result<List<TranscriptionHistoryItem>> = execute {
        dao.getExpiredByRetentionPolicy(policyId, cutoffTime).map { it.toDomainModel() }
    }

    override suspend fun bulkDelete(ids: List<String>): Result<Int> = execute {
        dao.deleteByIds(ids)
    }

    override suspend fun setProtectionStatus(
        ids: List<String>,
        isProtected: Boolean
    ): Result<Int> = execute {
        dao.setProtectionStatus(ids, isProtected)
    }

    override suspend fun getCountByRetentionPolicy(policyId: String): Result<Long> = execute {
        dao.getCountByRetentionPolicy(policyId)
    }

    override suspend fun deleteExpiredByRetentionPolicy(
        policyId: String,
        cutoffTime: Long
    ): Result<Int> = execute {
        dao.deleteExpiredByRetentionPolicy(policyId, cutoffTime)
    }

    override suspend fun getExportCount(
        dateRange: DateRange,
        retentionPolicyId: String?
    ): Result<Long> = execute {
        // For now, use the existing export count method
        // TODO: Enhance to support retention policy filtering
        dao.getExportCount(dateRange.startTime, dateRange.endTime)
    }

    override suspend fun updateRetentionPolicy(
        policy: me.shadykhalifa.whispertop.domain.models.RetentionPolicy
    ): Result<Int> = execute {
        // Update all transcriptions to use the new retention policy
        dao.updateAllRetentionPolicy(policy.id)
    }

    override suspend fun getDataSummary(): Result<me.shadykhalifa.whispertop.domain.models.DataSummary> = execute {
        val totalCount = dao.getTotalCount()
        val totalSize = dao.getTotalSizeBytes()
        val oldestDate = dao.getOldestTranscriptionDate()
        val newestDate = dao.getNewestTranscriptionDate()
        val protectedCount = dao.getProtectedCount()
        
        // Get counts by retention policy  
        val retentionPolicyCounts = mutableMapOf<me.shadykhalifa.whispertop.domain.models.RetentionPolicy, Int>()
        for (policy in me.shadykhalifa.whispertop.domain.models.RetentionPolicy.getAllPolicies()) {
            val count = dao.getCountByRetentionPolicy(policy.id).toInt()
            retentionPolicyCounts[policy] = count
        }
        
        me.shadykhalifa.whispertop.domain.models.DataSummary(
            totalTranscriptions = totalCount.toInt(),
            totalSizeBytes = totalSize,
            oldestTranscription = oldestDate?.let { java.time.LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)) },
            newestTranscription = newestDate?.let { java.time.LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000)) },
            protectedItems = protectedCount.toInt(),
            itemsByRetentionPolicy = retentionPolicyCounts
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
            model = model,
            wordCount = wordCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            retentionPolicyId = retentionPolicyId,
            isProtected = isProtected,
            exportCount = exportCount,
            lastExported = lastExported
        )
    }
}