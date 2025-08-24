package me.shadykhalifa.whispertop.data.services

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.shadykhalifa.whispertop.domain.services.AuditLogger
import me.shadykhalifa.whispertop.domain.services.DataOperation
import me.shadykhalifa.whispertop.domain.services.AuditLog
import me.shadykhalifa.whispertop.domain.services.AuditFilter
import me.shadykhalifa.whispertop.domain.services.AuditExportData
import me.shadykhalifa.whispertop.domain.services.AuditStatistics
import me.shadykhalifa.whispertop.domain.services.OperationType
import me.shadykhalifa.whispertop.domain.services.DataType
import me.shadykhalifa.whispertop.domain.services.AuditSeverity
import me.shadykhalifa.whispertop.utils.Result
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class AuditLoggerImpl(
    private val context: Context,
    private val json: Json
) : AuditLogger {
    
    private companion object {
        const val PREFS_NAME = "audit_logs"
        const val KEY_AUDIT_LOGS = "audit_logs_data"
        const val KEY_AUDIT_STATS = "audit_statistics"
        const val MAX_LOGS_IN_MEMORY = 10000 // Prevent unbounded growth
    }
    
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _criticalEvents = MutableSharedFlow<AuditLog>()
    
    override suspend fun logDataOperation(operation: DataOperation): Result<Boolean> = try {
        val auditLog = AuditLog(
            id = UUID.randomUUID().toString(),
            timestamp = operation.timestamp,
            operationType = operation.operationType,
            dataType = operation.dataType,
            recordId = operation.recordId,
            recordCount = operation.recordCount,
            userId = operation.userId,
            ipAddress = operation.ipAddress,
            userAgent = operation.userAgent,
            details = operation.details,
            success = operation.success,
            errorMessage = operation.errorMessage,
            sessionId = generateSessionId(),
            severity = determineSeverity(operation)
        )
        
        // Add to logs
        val currentLogs = getCurrentLogs().toMutableList()
        currentLogs.add(auditLog)
        
        // Maintain log size limit
        if (currentLogs.size > MAX_LOGS_IN_MEMORY) {
            currentLogs.removeAt(0)
        }
        
        // Save logs
        val logsData = currentLogs.map { it.toAuditLogData() }
        sharedPreferences.edit()
            .putString(KEY_AUDIT_LOGS, json.encodeToString(logsData))
            .apply()
        
        // Update statistics
        updateStatistics(auditLog)
        
        // Emit critical events
        if (auditLog.severity == AuditSeverity.CRITICAL || auditLog.severity == AuditSeverity.ERROR) {
            _criticalEvents.emit(auditLog)
        }
        
        Result.Success(true)
    } catch (exception: Exception) {
        Result.Error(exception)
    }
    
    override suspend fun getAuditLogs(filter: AuditFilter): Result<List<AuditLog>> = try {
        val allLogs = getCurrentLogs()
        val filteredLogs = allLogs.filter { log ->
            (filter.startDate == null || log.timestamp >= filter.startDate) &&
            (filter.endDate == null || log.timestamp <= filter.endDate) &&
            (filter.operationType == null || log.operationType == filter.operationType) &&
            (filter.dataType == null || log.dataType == filter.dataType) &&
            (filter.userId == null || log.userId == filter.userId) &&
            (filter.success == null || log.success == filter.success) &&
            (filter.severity == null || log.severity == filter.severity)
        }
        
        val pagedLogs = filteredLogs.drop(filter.offset).take(filter.limit)
        Result.Success(pagedLogs)
    } catch (exception: Exception) {
        Result.Error(exception)
    }
    
    override suspend fun exportAuditLogs(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Result<AuditExportData> {
        return try {
        val filter = AuditFilter(
            startDate = startDate,
            endDate = endDate,
            limit = Int.MAX_VALUE
        )
        
        val logsResult = getAuditLogs(filter)
        if (logsResult is Result.Error) {
            return Result.Error(logsResult.exception)
        }
        
        val logs = (logsResult as Result.Success).data
        val exportData = AuditExportData(
            logs = logs,
            exportTimestamp = LocalDateTime.now(),
            totalCount = logs.size,
            filter = filter
        )
        
        return Result.Success(exportData)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    override suspend fun cleanupOldLogs(retentionDays: Int): Result<Int> = try {
        val cutoffDate = LocalDateTime.now().minusDays(retentionDays.toLong())
        val allLogs = getCurrentLogs()
        val remainingLogs = allLogs.filter { it.timestamp >= cutoffDate }
        val deletedCount = allLogs.size - remainingLogs.size
        
        if (deletedCount > 0) {
            val logsData = remainingLogs.map { it.toAuditLogData() }
            sharedPreferences.edit()
                .putString(KEY_AUDIT_LOGS, json.encodeToString(logsData))
                .apply()
        }
        
        Result.Success(deletedCount)
    } catch (exception: Exception) {
        Result.Error(exception)
    }
    
    override suspend fun getAuditStatistics(): Result<AuditStatistics> = try {
        val statsJson = sharedPreferences.getString(KEY_AUDIT_STATS, null)
        if (statsJson != null) {
            val statsData = json.decodeFromString<AuditStatisticsData>(statsJson)
            Result.Success(statsData.toAuditStatistics())
        } else {
            val emptyStats = AuditStatistics(
                totalOperations = 0,
                operationsByType = emptyMap(),
                operationsByDataType = emptyMap(),
                successRate = 0.0,
                lastOperation = null,
                criticalEventsLast24h = 0
            )
            Result.Success(emptyStats)
        }
    } catch (exception: Exception) {
        Result.Error(exception)
    }
    
    override fun observeCriticalEvents(): Flow<AuditLog> = _criticalEvents.asSharedFlow()
    
    private fun getCurrentLogs(): List<AuditLog> {
        return try {
            val logsJson = sharedPreferences.getString(KEY_AUDIT_LOGS, "[]")
            val logsData = json.decodeFromString<List<AuditLogData>>(logsJson ?: "[]")
            logsData.map { it.toAuditLog() }
        } catch (exception: Exception) {
            emptyList()
        }
    }
    
    private suspend fun updateStatistics(newLog: AuditLog) {
        try {
            val currentStatsResult = getAuditStatistics()
            val currentStats = if (currentStatsResult is Result.Success) {
                currentStatsResult.data
            } else {
                AuditStatistics(
                    totalOperations = 0,
                    operationsByType = emptyMap(),
                    operationsByDataType = emptyMap(),
                    successRate = 0.0,
                    lastOperation = null,
                    criticalEventsLast24h = 0
                )
            }
            
            val updatedOperationsByType = currentStats.operationsByType.toMutableMap()
            updatedOperationsByType[newLog.operationType] = 
                updatedOperationsByType.getOrDefault(newLog.operationType, 0) + 1
            
            val updatedOperationsByDataType = currentStats.operationsByDataType.toMutableMap()
            updatedOperationsByDataType[newLog.dataType] = 
                updatedOperationsByDataType.getOrDefault(newLog.dataType, 0) + 1
            
            val totalOps = currentStats.totalOperations + 1
            val successCount = if (newLog.success) {
                (currentStats.successRate * currentStats.totalOperations + 1)
            } else {
                (currentStats.successRate * currentStats.totalOperations)
            }
            
            val criticalEvents24h = if (newLog.severity in listOf(AuditSeverity.CRITICAL, AuditSeverity.ERROR) &&
                newLog.timestamp >= LocalDateTime.now().minusDays(1)) {
                currentStats.criticalEventsLast24h + 1
            } else {
                currentStats.criticalEventsLast24h
            }
            
            val updatedStats = AuditStatistics(
                totalOperations = totalOps,
                operationsByType = updatedOperationsByType,
                operationsByDataType = updatedOperationsByDataType,
                successRate = successCount / totalOps,
                lastOperation = newLog.timestamp,
                criticalEventsLast24h = criticalEvents24h
            )
            
            val statsData = updatedStats.toAuditStatisticsData()
            sharedPreferences.edit()
                .putString(KEY_AUDIT_STATS, json.encodeToString(statsData))
                .apply()
        } catch (exception: Exception) {
            // Silently fail statistics updates to not break audit logging
            exception.printStackTrace()
        }
    }
    
    private fun determineSeverity(operation: DataOperation): AuditSeverity {
        return when {
            !operation.success -> AuditSeverity.ERROR
            operation.operationType in listOf(
                OperationType.DELETE,
                OperationType.PURGE,
                OperationType.CONSENT_CHANGE
            ) -> AuditSeverity.WARNING
            operation.operationType == OperationType.ERROR -> AuditSeverity.CRITICAL
            else -> AuditSeverity.INFO
        }
    }
    
    private fun generateSessionId(): String {
        // Simple session ID based on timestamp and random component
        return "${System.currentTimeMillis()}-${UUID.randomUUID().toString().take(8)}"
    }
}

// Data classes for serialization
@kotlinx.serialization.Serializable
data class AuditLogData(
    val id: String,
    val timestamp: String,
    val operationType: String,
    val dataType: String,
    val recordId: String?,
    val recordCount: Int,
    val userId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val details: Map<String, String>,
    val success: Boolean,
    val errorMessage: String?,
    val sessionId: String?,
    val severity: String
) {
    fun toAuditLog(): AuditLog = AuditLog(
        id = id,
        timestamp = LocalDateTime.parse(timestamp),
        operationType = OperationType.valueOf(operationType),
        dataType = DataType.valueOf(dataType),
        recordId = recordId,
        recordCount = recordCount,
        userId = userId,
        ipAddress = ipAddress,
        userAgent = userAgent,
        details = details,
        success = success,
        errorMessage = errorMessage,
        sessionId = sessionId,
        severity = AuditSeverity.valueOf(severity)
    )
}

@kotlinx.serialization.Serializable
data class AuditStatisticsData(
    val totalOperations: Long,
    val operationsByType: Map<String, Long>,
    val operationsByDataType: Map<String, Long>,
    val successRate: Double,
    val lastOperation: String?,
    val criticalEventsLast24h: Long
) {
    fun toAuditStatistics(): AuditStatistics = AuditStatistics(
        totalOperations = totalOperations,
        operationsByType = operationsByType.mapKeys { OperationType.valueOf(it.key) },
        operationsByDataType = operationsByDataType.mapKeys { DataType.valueOf(it.key) },
        successRate = successRate,
        lastOperation = lastOperation?.let { LocalDateTime.parse(it) },
        criticalEventsLast24h = criticalEventsLast24h
    )
}

private fun AuditLog.toAuditLogData(): AuditLogData = AuditLogData(
    id = id,
    timestamp = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    operationType = operationType.name,
    dataType = dataType.name,
    recordId = recordId,
    recordCount = recordCount,
    userId = userId,
    ipAddress = ipAddress,
    userAgent = userAgent,
    details = details,
    success = success,
    errorMessage = errorMessage,
    sessionId = sessionId,
    severity = severity.name
)

private fun AuditStatistics.toAuditStatisticsData(): AuditStatisticsData = AuditStatisticsData(
    totalOperations = totalOperations,
    operationsByType = operationsByType.mapKeys { it.key.name },
    operationsByDataType = operationsByDataType.mapKeys { it.key.name },
    successRate = successRate,
    lastOperation = lastOperation?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    criticalEventsLast24h = criticalEventsLast24h
)