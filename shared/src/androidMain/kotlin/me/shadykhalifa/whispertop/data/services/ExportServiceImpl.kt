package me.shadykhalifa.whispertop.data.services

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.domain.models.DateRange
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ExportResult
import me.shadykhalifa.whispertop.domain.models.TranscriptionHistoryItem
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionDatabaseRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.services.ExportService
import me.shadykhalifa.whispertop.domain.services.ExportStatistics
import me.shadykhalifa.whispertop.utils.Result
import java.io.File
import java.io.FileWriter
import java.io.IOException

class ExportServiceImpl(
    private val context: Context,
    private val transcriptionRepository: TranscriptionDatabaseRepository,
    private val historyRepository: TranscriptionHistoryRepository
) : ExportService {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val exportDirectory: File by lazy {
        File(context.filesDir, "exports").apply {
            if (!exists()) mkdirs()
        }
    }

    private val tempExportDirectory: File by lazy {
        File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun exportTranscriptions(
        format: ExportFormat,
        dateRange: DateRange,
        retentionPolicyId: String?,
        progressCallback: ((Int) -> Unit)?
    ): Flow<ExportResult> = flow {
        emit(ExportResult.InProgress(0.0f))
        
        try {
            // Get total count for progress tracking
            val totalCountResult = transcriptionRepository.getExportCount(dateRange, retentionPolicyId)
            if (totalCountResult is Result.Error) {
                emit(ExportResult.Error("Failed to get export count", totalCountResult.exception))
                return@flow
            }
            
            val totalCount = (totalCountResult as Result.Success).data
            if (totalCount == 0L) {
                val fileName = generateFileName(format, "empty")
                emit(ExportResult.Success(fileName, 0))
                return@flow
            }
            
            val allTranscriptions = mutableListOf<TranscriptionHistoryItem>()
            val chunkSize = 1000
            var offset = 0
            
            // Fetch data in chunks to avoid memory issues
            while (offset < totalCount) {
                val chunkResult = transcriptionRepository.getAllForExport(
                    dateRange = dateRange,
                    retentionPolicyId = retentionPolicyId,
                    limit = chunkSize,
                    offset = offset
                )
                
                when (chunkResult) {
                    is Result.Success -> {
                        val chunk = chunkResult.data
                        if (chunk.isEmpty()) break
                        
                        allTranscriptions.addAll(chunk)
                        offset += chunk.size
                        
                        // Report progress
                        if (totalCount > chunkSize) {
                            val progress = (offset.toFloat() / totalCount * 50).toInt() // Use 50% for data loading
                            progressCallback?.invoke(progress)
                        }
                    }
                    is Result.Error -> {
                        emit(ExportResult.Error("Failed to fetch transcriptions", chunkResult.exception))
                        return@flow
                    }
                    else -> break
                }
            }
            
            // Generate export content
            progressCallback?.invoke(75) // Data processing phase
            val fileName = generateFileName(format)
            val file = File(exportDirectory, fileName)
            
            val success = when (format) {
                ExportFormat.JSON -> writeJsonFile(file, allTranscriptions)
                ExportFormat.CSV -> writeCsvFile(file, allTranscriptions)
                ExportFormat.TXT -> writeTxtFile(file, allTranscriptions)
                else -> false
            }
            
            if (success) {
                // Mark transcriptions as exported
                val ids = allTranscriptions.map { it.id }
                transcriptionRepository.markAsExported(ids)
                
                progressCallback?.invoke(100)
                emit(ExportResult.Success(fileName, allTranscriptions.size))
            } else {
                emit(ExportResult.Error("Failed to write export file"))
            }
            
        } catch (e: Exception) {
            emit(ExportResult.Error("Export failed: ${e.message}", e))
        }
    }

    override suspend fun exportTranscriptionsByIds(
        ids: List<String>,
        format: ExportFormat,
        progressCallback: ((Int) -> Unit)?
    ): Flow<ExportResult> = flow {
        emit(ExportResult.InProgress(0.0f))
        
        try {
            val transcriptions = mutableListOf<TranscriptionHistoryItem>()
            
            // Fetch transcriptions by ID
            for ((index, id) in ids.withIndex()) {
                val result = historyRepository.getTranscription(id)
                when (result) {
                    is Result.Success -> {
                        result.data?.let { transcriptions.add(it) }
                    }
                    is Result.Error -> {
                        emit(ExportResult.Error("Failed to fetch transcription $id", result.exception))
                        return@flow
                    }
                    else -> {}
                }
                
                progressCallback?.invoke((index + 1) * 50 / ids.size)
            }
            
            if (transcriptions.isEmpty()) {
                emit(ExportResult.Success("empty_export.${format.extension}", 0))
                return@flow
            }
            
            // Generate export file
            val fileName = generateFileName(format)
            val file = File(exportDirectory, fileName)
            
            val success = when (format) {
                ExportFormat.JSON -> writeJsonFile(file, transcriptions)
                ExportFormat.CSV -> writeCsvFile(file, transcriptions)
                ExportFormat.TXT -> writeTxtFile(file, transcriptions)
                else -> false
            }
            
            if (success) {
                // Mark as exported
                transcriptionRepository.markAsExported(transcriptions.map { it.id })
                
                progressCallback?.invoke(100)
                emit(ExportResult.Success(fileName, transcriptions.size))
            } else {
                emit(ExportResult.Error("Failed to write export file"))
            }
            
        } catch (e: Exception) {
            emit(ExportResult.Error("Export by IDs failed: ${e.message}", e))
        }
    }

    override suspend fun createSecureFileUri(fileName: String): Result<String> {
        return try {
            val file = File(exportDirectory, fileName)
            if (!file.exists()) {
                return Result.Error(IOException("Export file not found: $fileName"))
            }
            
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            
            Result.Success(uri.toString())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cleanupTemporaryFiles(): Result<Int> {
        return try {
            val tempFiles = tempExportDirectory.listFiles()
            var deletedCount = 0
            
            tempFiles?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }
            
            // Also clean up old export files (older than 7 days)
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val exportFiles = exportDirectory.listFiles()
            
            exportFiles?.forEach { file ->
                if (file.lastModified() < sevenDaysAgo && file.delete()) {
                    deletedCount++
                }
            }
            
            Result.Success(deletedCount)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getExportStatistics(): Result<ExportStatistics> {
        // TODO: Implement proper statistics tracking
        // For now, return basic statistics
        return try {
            val exportFiles = exportDirectory.listFiles() ?: emptyArray()
            val totalExports = exportFiles.size.toLong()
            val lastExportTime = exportFiles.maxOfOrNull { it.lastModified() }
            val averageSize = if (exportFiles.isNotEmpty()) {
                exportFiles.sumOf { it.length() } / exportFiles.size
            } else 0L
            
            val formatCounts = mapOf(
                ExportFormat.JSON to exportFiles.count { it.name.endsWith(".json") }.toLong(),
                ExportFormat.CSV to exportFiles.count { it.name.endsWith(".csv") }.toLong(),
                ExportFormat.TXT to exportFiles.count { it.name.endsWith(".txt") }.toLong()
            )
            
            val statistics = ExportStatistics(
                totalExports = totalExports,
                totalItemsExported = 0L, // TODO: Track from metadata
                lastExportTime = lastExportTime,
                averageExportSize = averageSize,
                popularFormats = formatCounts
            )
            
            Result.Success(statistics)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    private fun generateFileName(format: ExportFormat, prefix: String = "transcriptions"): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        return "${prefix}_${timestamp}.${format.extension}"
    }

    private fun writeJsonFile(file: File, transcriptions: List<TranscriptionHistoryItem>): Boolean {
        return try {
            val jsonContent = json.encodeToString(transcriptions)
            FileWriter(file).use { writer ->
                writer.write(jsonContent)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeCsvFile(file: File, transcriptions: List<TranscriptionHistoryItem>): Boolean {
        return try {
            FileWriter(file).use { writer ->
                // Write CSV header
                writer.write("ID,Text,Timestamp,Duration,Language,Model,WordCount,CreatedAt,ConfidenceScore\n")
                
                // Write data rows
                transcriptions.forEach { transcription ->
                    val row = listOf(
                        escapeCsvField(transcription.id),
                        escapeCsvField(transcription.text),
                        transcription.timestamp.toString(),
                        transcription.duration?.toString() ?: "",
                        escapeCsvField(transcription.language ?: ""),
                        escapeCsvField(transcription.model ?: ""),
                        transcription.wordCount.toString(),
                        transcription.createdAt.toString(),
                        transcription.confidence?.toString() ?: ""
                    ).joinToString(",")
                    
                    writer.write("$row\n")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun writeTxtFile(file: File, transcriptions: List<TranscriptionHistoryItem>): Boolean {
        return try {
            FileWriter(file).use { writer ->
                writer.write("WhisperTop Transcription Export\n")
                writer.write("Generated: ${java.util.Date()}\n")
                writer.write("Total Transcriptions: ${transcriptions.size}\n")
                writer.write("=" .repeat(50) + "\n\n")
                
                transcriptions.forEach { transcription ->
                    writer.write("Transcription ID: ${transcription.id}\n")
                    writer.write("Date: ${java.util.Date(transcription.timestamp)}\n")
                    writer.write("Duration: ${transcription.duration ?: "N/A"}s\n")
                    writer.write("Language: ${transcription.language ?: "Unknown"}\n")
                    writer.write("Model: ${transcription.model ?: "Unknown"}\n")
                    writer.write("Confidence: ${transcription.confidence ?: "N/A"}\n")
                    writer.write("Word Count: ${transcription.wordCount}\n")
                    writer.write("\nText:\n${transcription.text}\n")
                    writer.write("-".repeat(50) + "\n\n")
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            "\"${field.replace("\"", "\"\"")}\""
        } else {
            field
        }
    }
}