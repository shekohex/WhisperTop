package me.shadykhalifa.whispertop.domain.models

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class RetentionPolicyResult(
    val sessionsDeleted: Int,
    val transcriptionsDeleted: Int,
    val bytesFreed: Long,
    val lastCleanupDate: LocalDate
) {
    val totalRecordsDeleted: Int
        get() = sessionsDeleted + transcriptionsDeleted
    
    val megabytesFreed: Double
        get() = bytesFreed / (1024.0 * 1024.0)
    
    fun formatBytesFreed(): String {
        return when {
            bytesFreed < 1024 -> "${bytesFreed}B"
            bytesFreed < 1024 * 1024 -> "${String.format("%.1f", bytesFreed / 1024.0)}KB"
            bytesFreed < 1024 * 1024 * 1024 -> "${String.format("%.1f", megabytesFreed)}MB"
            else -> "${String.format("%.1f", bytesFreed / (1024.0 * 1024.0 * 1024.0))}GB"
        }
    }
}