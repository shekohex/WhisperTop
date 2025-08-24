package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.utils.Result
import java.time.LocalDateTime

/**
 * Audit logging service for GDPR compliance and security monitoring
 */
interface AuditLogger {
    
    /**
     * Log a data operation
     */
    suspend fun logDataOperation(operation: DataOperation): Result<Boolean>
    
    /**
     * Get audit logs with filtering
     */
    suspend fun getAuditLogs(filter: AuditFilter): Result<List<AuditLog>>
    
    /**
     * Export audit logs for compliance reporting
     */
    suspend fun exportAuditLogs(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Result<AuditExportData>
    
    /**
     * Clear old audit logs based on retention policy
     */
    suspend fun cleanupOldLogs(retentionDays: Int): Result<Int>
    
    /**
     * Get audit statistics
     */
    suspend fun getAuditStatistics(): Result<AuditStatistics>
    
    /**
     * Observe critical audit events
     */
    fun observeCriticalEvents(): Flow<AuditLog>
}

data class DataOperation(
    val operationType: OperationType,
    val dataType: DataType,
    val recordId: String? = null,
    val recordCount: Int = 1,
    val userId: String? = null,
    val ipAddress: String? = null,
    val userAgent: String? = null,
    val details: Map<String, String> = emptyMap(),
    val success: Boolean = true,
    val errorMessage: String? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class AuditLog(
    val id: String,
    val timestamp: LocalDateTime,
    val operationType: OperationType,
    val dataType: DataType,
    val recordId: String?,
    val recordCount: Int,
    val userId: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val details: Map<String, String>,
    val success: Boolean,
    val errorMessage: String?,
    val sessionId: String? = null,
    val severity: AuditSeverity = AuditSeverity.INFO
)

data class AuditFilter(
    val startDate: LocalDateTime? = null,
    val endDate: LocalDateTime? = null,
    val operationType: OperationType? = null,
    val dataType: DataType? = null,
    val userId: String? = null,
    val success: Boolean? = null,
    val severity: AuditSeverity? = null,
    val limit: Int = 1000,
    val offset: Int = 0
)

data class AuditExportData(
    val logs: List<AuditLog>,
    val exportTimestamp: LocalDateTime,
    val totalCount: Int,
    val filter: AuditFilter,
    val exportedBy: String = "system",
    val complianceNote: String = "Export for GDPR compliance and audit purposes"
)

data class AuditStatistics(
    val totalOperations: Long,
    val operationsByType: Map<OperationType, Long>,
    val operationsByDataType: Map<DataType, Long>,
    val successRate: Double,
    val lastOperation: LocalDateTime?,
    val criticalEventsLast24h: Long
)

enum class OperationType {
    CREATE,
    READ,
    UPDATE,
    DELETE,
    EXPORT,
    IMPORT,
    BACKUP,
    RESTORE,
    PURGE,
    CONSENT_CHANGE,
    LOGIN,
    LOGOUT,
    SYSTEM_START,
    SYSTEM_STOP,
    ERROR
}

enum class DataType {
    TRANSCRIPTION,
    AUDIO_FILE,
    USER_SETTINGS,
    CONSENT_RECORD,
    AUDIT_LOG,
    EXPORT_FILE,
    BACKUP_FILE,
    SYSTEM_CONFIG,
    USER_SESSION
}

enum class AuditSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}