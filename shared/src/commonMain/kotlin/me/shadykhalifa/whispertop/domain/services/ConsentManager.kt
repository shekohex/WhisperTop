package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.utils.Result
import java.time.LocalDateTime

/**
 * GDPR consent management interface
 */
interface ConsentManager {
    
    /**
     * Get current consent status
     */
    suspend fun getConsentStatus(): Result<ConsentStatus>
    
    /**
     * Update consent for specific purposes
     */
    suspend fun updateConsent(consent: ConsentUpdate): Result<Boolean>
    
    /**
     * Get consent history for audit purposes
     */
    suspend fun getConsentHistory(): Result<List<ConsentRecord>>
    
    /**
     * Withdraw all consent (right to be forgotten)
     */
    suspend fun withdrawAllConsent(): Result<Boolean>
    
    /**
     * Check if specific data processing is consented
     */
    suspend fun isConsentedFor(purpose: DataProcessingPurpose): Result<Boolean>
    
    /**
     * Observe consent changes
     */
    fun observeConsentChanges(): Flow<ConsentStatus>
    
    /**
     * Export consent data for data portability
     */
    suspend fun exportConsentData(): Result<ConsentExportData>
    
    /**
     * Initialize consent status for new users
     */
    suspend fun initializeConsent(initialConsent: ConsentStatus): Result<Boolean>
}

data class ConsentStatus(
    val dataCollection: Boolean = false,
    val dataProcessing: Boolean = false,
    val dataStorage: Boolean = false,
    val dataExport: Boolean = false,
    val analytics: Boolean = false,
    val improvement: Boolean = false,
    val lastUpdated: LocalDateTime? = null,
    val version: String = "1.0"
)

data class ConsentUpdate(
    val dataCollection: Boolean? = null,
    val dataProcessing: Boolean? = null,
    val dataStorage: Boolean? = null,
    val dataExport: Boolean? = null,
    val analytics: Boolean? = null,
    val improvement: Boolean? = null,
    val timestamp: LocalDateTime = LocalDateTime.now()
)

data class ConsentRecord(
    val id: String,
    val consentStatus: ConsentStatus,
    val timestamp: LocalDateTime,
    val method: String, // "user_action", "initial_setup", "withdrawal", etc.
    val ipAddress: String? = null,
    val userAgent: String? = null
)

data class ConsentExportData(
    val currentStatus: ConsentStatus,
    val consentHistory: List<ConsentRecord>,
    val exportTimestamp: LocalDateTime,
    val dataController: String = "WhisperTop",
    val legalBasis: Map<DataProcessingPurpose, String>
)

enum class DataProcessingPurpose {
    TRANSCRIPTION,
    HISTORY_STORAGE,
    DATA_EXPORT,
    PERFORMANCE_ANALYTICS,
    SERVICE_IMPROVEMENT,
    ERROR_REPORTING
}