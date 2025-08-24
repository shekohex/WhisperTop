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
import me.shadykhalifa.whispertop.domain.services.ConsentManager
import me.shadykhalifa.whispertop.domain.services.ConsentStatus
import me.shadykhalifa.whispertop.domain.services.ConsentUpdate
import me.shadykhalifa.whispertop.domain.services.ConsentRecord
import me.shadykhalifa.whispertop.domain.services.ConsentExportData
import me.shadykhalifa.whispertop.domain.services.DataProcessingPurpose
import me.shadykhalifa.whispertop.utils.Result
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class ConsentManagerImpl(
    private val context: Context,
    private val json: Json
) : ConsentManager {
    
    private companion object {
        const val PREFS_NAME = "consent_preferences"
        const val KEY_CURRENT_CONSENT = "current_consent"
        const val KEY_CONSENT_HISTORY = "consent_history"
        const val KEY_CONSENT_VERSION = "consent_version"
        const val CURRENT_CONSENT_VERSION = "1.0"
    }
    
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    
    private val sharedPreferences: SharedPreferences = EncryptedSharedPreferences.create(
        PREFS_NAME,
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val _consentChanges = MutableSharedFlow<ConsentStatus>(replay = 1)
    
    override suspend fun getConsentStatus(): Result<ConsentStatus> = try {
        val consentJson = sharedPreferences.getString(KEY_CURRENT_CONSENT, null)
        val status = if (consentJson != null) {
            json.decodeFromString<ConsentStatusData>(consentJson).toConsentStatus()
        } else {
            ConsentStatus() // Default: no consent given
        }
        Result.Success(status)
    } catch (exception: Exception) {
        Result.Error(exception)
    }
    
    override suspend fun updateConsent(consent: ConsentUpdate): Result<Boolean> {
        return try {
        val currentStatus = getConsentStatus()
        if (currentStatus is Result.Error) {
            return Result.Error(currentStatus.exception)
        }
        
        val current = (currentStatus as Result.Success).data
        val updated = current.copy(
            dataCollection = consent.dataCollection ?: current.dataCollection,
            dataProcessing = consent.dataProcessing ?: current.dataProcessing,
            dataStorage = consent.dataStorage ?: current.dataStorage,
            dataExport = consent.dataExport ?: current.dataExport,
            analytics = consent.analytics ?: current.analytics,
            improvement = consent.improvement ?: current.improvement,
            lastUpdated = consent.timestamp,
            version = CURRENT_CONSENT_VERSION
        )
        
        // Save updated consent
        val statusData = updated.toConsentStatusData()
        sharedPreferences.edit()
            .putString(KEY_CURRENT_CONSENT, json.encodeToString(statusData))
            .apply()
        
        // Record consent history
        recordConsentChange(updated, "user_action", null, null)
        
        // Notify observers
        _consentChanges.emit(updated)
        
            Result.Success(true)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    override suspend fun getConsentHistory(): Result<List<ConsentRecord>> {
        return try {
        val historyJson = sharedPreferences.getString(KEY_CONSENT_HISTORY, "[]") ?: "[]"
        val historyData = json.decodeFromString<List<ConsentRecordData>>(historyJson)
        val history = historyData.map { it.toConsentRecord() }
            Result.Success(history)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    override suspend fun withdrawAllConsent(): Result<Boolean> {
        return try {
        val withdrawnConsent = ConsentStatus(
            dataCollection = false,
            dataProcessing = false,
            dataStorage = false,
            dataExport = false,
            analytics = false,
            improvement = false,
            lastUpdated = LocalDateTime.now(),
            version = CURRENT_CONSENT_VERSION
        )
        
        val statusData = withdrawnConsent.toConsentStatusData()
        sharedPreferences.edit()
            .putString(KEY_CURRENT_CONSENT, json.encodeToString(statusData))
            .apply()
        
        recordConsentChange(withdrawnConsent, "full_withdrawal", null, null)
        _consentChanges.emit(withdrawnConsent)
        
            Result.Success(true)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    override suspend fun isConsentedFor(purpose: DataProcessingPurpose): Result<Boolean> {
        return try {
        val statusResult = getConsentStatus()
        if (statusResult is Result.Error) {
            return Result.Error(statusResult.exception)
        }
        
        val status = (statusResult as Result.Success).data
        val isConsented = when (purpose) {
            DataProcessingPurpose.TRANSCRIPTION -> status.dataProcessing
            DataProcessingPurpose.HISTORY_STORAGE -> status.dataStorage
            DataProcessingPurpose.DATA_EXPORT -> status.dataExport
            DataProcessingPurpose.PERFORMANCE_ANALYTICS -> status.analytics
            DataProcessingPurpose.SERVICE_IMPROVEMENT -> status.improvement
            DataProcessingPurpose.ERROR_REPORTING -> status.analytics
        }
        
            Result.Success(isConsented)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    override fun observeConsentChanges(): Flow<ConsentStatus> = _consentChanges.asSharedFlow()
    
    override suspend fun exportConsentData(): Result<ConsentExportData> {
        return try {
        val statusResult = getConsentStatus()
        val historyResult = getConsentHistory()
        
        if (statusResult is Result.Error) return Result.Error(statusResult.exception)
        if (historyResult is Result.Error) return Result.Error(historyResult.exception)
        
        val exportData = ConsentExportData(
            currentStatus = (statusResult as Result.Success).data,
            consentHistory = (historyResult as Result.Success).data,
            exportTimestamp = LocalDateTime.now(),
            legalBasis = mapOf(
                DataProcessingPurpose.TRANSCRIPTION to "User Consent (Article 6(1)(a) GDPR)",
                DataProcessingPurpose.HISTORY_STORAGE to "User Consent (Article 6(1)(a) GDPR)",
                DataProcessingPurpose.DATA_EXPORT to "User Consent (Article 6(1)(a) GDPR)",
                DataProcessingPurpose.PERFORMANCE_ANALYTICS to "User Consent (Article 6(1)(a) GDPR)",
                DataProcessingPurpose.SERVICE_IMPROVEMENT to "User Consent (Article 6(1)(a) GDPR)",
                DataProcessingPurpose.ERROR_REPORTING to "Legitimate Interest (Article 6(1)(f) GDPR)"
            )
        )
        
            Result.Success(exportData)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    override suspend fun initializeConsent(initialConsent: ConsentStatus): Result<Boolean> {
        return try {
        // Only initialize if no consent exists
        val existing = sharedPreferences.getString(KEY_CURRENT_CONSENT, null)
        if (existing != null) {
            return Result.Success(true) // Already initialized
        }
        
        val statusData = initialConsent.copy(
            lastUpdated = LocalDateTime.now(),
            version = CURRENT_CONSENT_VERSION
        ).toConsentStatusData()
        
        sharedPreferences.edit()
            .putString(KEY_CURRENT_CONSENT, json.encodeToString(statusData))
            .apply()
        
        recordConsentChange(initialConsent, "initialization", null, null)
        _consentChanges.emit(initialConsent)
        
            Result.Success(true)
        } catch (exception: Exception) {
            Result.Error(exception)
        }
    }
    
    private suspend fun recordConsentChange(
        consentStatus: ConsentStatus,
        method: String,
        ipAddress: String? = null,
        userAgent: String? = null
    ) {
        try {
            val historyResult = getConsentHistory()
            val currentHistory = if (historyResult is Result.Success) {
                historyResult.data.toMutableList()
            } else {
                mutableListOf()
            }
            
            val record = ConsentRecord(
                id = UUID.randomUUID().toString(),
                consentStatus = consentStatus,
                timestamp = LocalDateTime.now(),
                method = method,
                ipAddress = ipAddress,
                userAgent = userAgent
            )
            
            currentHistory.add(record)
            
            // Keep only last 100 records to prevent unbounded growth
            if (currentHistory.size > 100) {
                currentHistory.removeAt(0)
            }
            
            val historyData = currentHistory.map { it.toConsentRecordData() }
            sharedPreferences.edit()
                .putString(KEY_CONSENT_HISTORY, json.encodeToString(historyData))
                .apply()
        } catch (exception: Exception) {
            // Silently fail consent history recording to not break consent updates
            exception.printStackTrace()
        }
    }
}

// Data classes for serialization
@kotlinx.serialization.Serializable
data class ConsentStatusData(
    val dataCollection: Boolean,
    val dataProcessing: Boolean,
    val dataStorage: Boolean,
    val dataExport: Boolean,
    val analytics: Boolean,
    val improvement: Boolean,
    val lastUpdated: String?,
    val version: String
) {
    fun toConsentStatus(): ConsentStatus = ConsentStatus(
        dataCollection = dataCollection,
        dataProcessing = dataProcessing,
        dataStorage = dataStorage,
        dataExport = dataExport,
        analytics = analytics,
        improvement = improvement,
        lastUpdated = lastUpdated?.let { LocalDateTime.parse(it) },
        version = version
    )
}

@kotlinx.serialization.Serializable
data class ConsentRecordData(
    val id: String,
    val consentStatus: ConsentStatusData,
    val timestamp: String,
    val method: String,
    val ipAddress: String? = null,
    val userAgent: String? = null
) {
    fun toConsentRecord(): ConsentRecord = ConsentRecord(
        id = id,
        consentStatus = consentStatus.toConsentStatus(),
        timestamp = LocalDateTime.parse(timestamp),
        method = method,
        ipAddress = ipAddress,
        userAgent = userAgent
    )
}

private fun ConsentStatus.toConsentStatusData(): ConsentStatusData = ConsentStatusData(
    dataCollection = dataCollection,
    dataProcessing = dataProcessing,
    dataStorage = dataStorage,
    dataExport = dataExport,
    analytics = analytics,
    improvement = improvement,
    lastUpdated = lastUpdated?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    version = version
)

private fun ConsentRecord.toConsentRecordData(): ConsentRecordData = ConsentRecordData(
    id = id,
    consentStatus = consentStatus.toConsentStatusData(),
    timestamp = timestamp.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
    method = method,
    ipAddress = ipAddress,
    userAgent = userAgent
)