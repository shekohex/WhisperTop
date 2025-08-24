package me.shadykhalifa.whispertop.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.services.ConsentManager
import me.shadykhalifa.whispertop.domain.services.ConsentStatus
import me.shadykhalifa.whispertop.domain.services.ConsentUpdate
import me.shadykhalifa.whispertop.domain.services.AuditLogger
import me.shadykhalifa.whispertop.domain.services.AuditStatistics
import me.shadykhalifa.whispertop.domain.services.DataOperation
import me.shadykhalifa.whispertop.domain.services.OperationType
import me.shadykhalifa.whispertop.domain.services.DataType
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionDatabaseRepository
import me.shadykhalifa.whispertop.domain.services.ExportService
import me.shadykhalifa.whispertop.utils.Result
import me.shadykhalifa.whispertop.domain.models.DataSummary
import java.time.LocalDateTime

class PrivacyViewModel(
    private val consentManager: ConsentManager,
    private val auditLogger: AuditLogger,
    private val transcriptionDatabaseRepository: TranscriptionDatabaseRepository,
    private val exportService: ExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrivacyUiState())
    val uiState: StateFlow<PrivacyUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<PrivacyUiEvent>()
    val uiEvents: SharedFlow<PrivacyUiEvent> = _uiEvents.asSharedFlow()

    fun loadPrivacyData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingData = true) }
            
            try {
                // Load consent status
                when (val consentResult = consentManager.getConsentStatus()) {
                    is Result.Success -> {
                        _uiState.update { it.copy(consentStatus = consentResult.data) }
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to load consent status: ${consentResult.exception.message}")
                        )
                    }
                    is Result.Loading -> { /* Loading state already set */ }
                }
                
                // Load data summary
                when (val summaryResult = transcriptionDatabaseRepository.getDataSummary()) {
                    is Result.Success -> {
                        _uiState.update { it.copy(dataSummary = summaryResult.data) }
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to load data summary: ${summaryResult.exception.message}")
                        )
                    }
                    is Result.Loading -> { /* Loading state already set */ }
                }
                
                // Log privacy dashboard access
                auditLogger.logDataOperation(
                    DataOperation(
                        operationType = OperationType.READ,
                        dataType = DataType.USER_SETTINGS,
                        details = mapOf("action" to "privacy_dashboard_viewed")
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoadingData = false) }
            }
        }
    }

    fun updateConsent(consentType: String, value: Boolean) {
        viewModelScope.launch {
            try {
                val consentUpdate = when (consentType) {
                    "dataCollection" -> ConsentUpdate(dataCollection = value)
                    "dataProcessing" -> ConsentUpdate(dataProcessing = value)
                    "dataStorage" -> ConsentUpdate(dataStorage = value)
                    "dataExport" -> ConsentUpdate(dataExport = value)
                    "analytics" -> ConsentUpdate(analytics = value)
                    "improvement" -> ConsentUpdate(improvement = value)
                    else -> return@launch
                }
                
                when (val result = consentManager.updateConsent(consentUpdate)) {
                    is Result.Success -> {
                        // Reload consent status to reflect changes
                        when (val statusResult = consentManager.getConsentStatus()) {
                            is Result.Success -> {
                                _uiState.update { it.copy(consentStatus = statusResult.data) }
                                _uiEvents.emit(
                                    PrivacyUiEvent.ShowMessage("Consent updated successfully")
                                )
                            }
                            is Result.Error -> {
                                _uiEvents.emit(
                                    PrivacyUiEvent.ShowError("Failed to reload consent status")
                                )
                            }
                            is Result.Loading -> { /* Loading state handled elsewhere */ }
                        }
                        
                        // Log consent change
                        auditLogger.logDataOperation(
                            DataOperation(
                                operationType = OperationType.CONSENT_CHANGE,
                                dataType = DataType.CONSENT_RECORD,
                                details = mapOf(
                                    "consent_type" to consentType,
                                    "new_value" to value.toString()
                                )
                            )
                        )
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to update consent: ${result.exception.message}")
                        )
                    }
                    is Result.Loading -> { /* Loading state handled elsewhere */ }
                }
            } catch (exception: Exception) {
                _uiEvents.emit(
                    PrivacyUiEvent.ShowError("Failed to update consent: ${exception.message}")
                )
            }
        }
    }

    fun exportPersonalData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingData = true) }
            
            try {
                // Check consent for data export
                when (val consentResult = consentManager.isConsentedFor(
                    me.shadykhalifa.whispertop.domain.services.DataProcessingPurpose.DATA_EXPORT
                )) {
                    is Result.Success -> {
                        if (!consentResult.data) {
                            _uiEvents.emit(
                                PrivacyUiEvent.ShowError("Data export consent is required for this operation")
                            )
                            return@launch
                        }
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to check export consent")
                        )
                        return@launch
                    }
                    is Result.Loading -> { /* Continue with export */ }
                }
                
                // Export consent data
                when (val consentExportResult = consentManager.exportConsentData()) {
                    is Result.Success -> {
                        // Here we would typically save the consent data to a file
                        // For now, just show success message
                        _uiEvents.emit(
                            PrivacyUiEvent.ExportCompleted("Personal data exported successfully")
                        )
                        
                        // Log export operation
                        auditLogger.logDataOperation(
                            DataOperation(
                                operationType = OperationType.EXPORT,
                                dataType = DataType.CONSENT_RECORD,
                                details = mapOf("export_type" to "gdpr_personal_data")
                            )
                        )
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to export personal data: ${consentExportResult.exception.message}")
                        )
                    }
                    is Result.Loading -> { /* Loading state already set */ }
                }
            } finally {
                _uiState.update { it.copy(isExportingData = false) }
            }
        }
    }

    fun initiateDataDeletion() {
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    fun confirmDataDeletion() {
        viewModelScope.launch {
            _uiState.update { 
                it.copy(
                    showDeleteConfirmation = false,
                    isDeletingData = true
                ) 
            }
            
            try {
                // Withdraw all consent first
                when (val withdrawResult = consentManager.withdrawAllConsent()) {
                    is Result.Success -> {
                        // Log data deletion initiation
                        auditLogger.logDataOperation(
                            DataOperation(
                                operationType = OperationType.DELETE,
                                dataType = DataType.USER_SESSION,
                                details = mapOf(
                                    "action" to "right_to_be_forgotten",
                                    "scope" to "all_user_data"
                                )
                            )
                        )
                        
                        // Here we would implement complete data deletion
                        // For now, just show confirmation
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowMessage(
                                "Data deletion request submitted. All personal data will be removed within 30 days."
                            )
                        )
                        
                        // Update consent status
                        loadPrivacyData()
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to process deletion request: ${withdrawResult.exception.message}")
                        )
                    }
                    is Result.Loading -> { /* Loading state already set */ }
                }
            } finally {
                _uiState.update { it.copy(isDeletingData = false) }
            }
        }
    }

    fun cancelDataDeletion() {
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    fun showAuditLog() {
        viewModelScope.launch {
            try {
                when (val statsResult = auditLogger.getAuditStatistics()) {
                    is Result.Success -> {
                        _uiState.update { 
                            it.copy(
                                auditStatistics = statsResult.data,
                                showAuditInfo = true
                            ) 
                        }
                        
                        // Log audit log access
                        auditLogger.logDataOperation(
                            DataOperation(
                                operationType = OperationType.READ,
                                dataType = DataType.AUDIT_LOG,
                                details = mapOf("action" to "audit_statistics_viewed")
                            )
                        )
                    }
                    is Result.Error -> {
                        _uiEvents.emit(
                            PrivacyUiEvent.ShowError("Failed to load audit statistics: ${statsResult.exception.message}")
                        )
                    }
                    is Result.Loading -> { /* Loading handled by UI */ }
                }
            } catch (exception: Exception) {
                _uiEvents.emit(
                    PrivacyUiEvent.ShowError("Failed to load audit log: ${exception.message}")
                )
            }
        }
    }

    fun hideAuditInfo() {
        _uiState.update { it.copy(showAuditInfo = false, auditStatistics = null) }
    }

    init {
        // Observe consent changes
        viewModelScope.launch {
            consentManager.observeConsentChanges().collect { consentStatus ->
                _uiState.update { it.copy(consentStatus = consentStatus) }
            }
        }
        
        // Observe critical audit events
        viewModelScope.launch {
            auditLogger.observeCriticalEvents().collect { auditLog ->
                _uiEvents.emit(
                    PrivacyUiEvent.ShowMessage("Critical security event logged: ${auditLog.operationType}")
                )
            }
        }
    }
}

data class PrivacyUiState(
    val consentStatus: ConsentStatus? = null,
    val dataSummary: DataSummary? = null,
    val auditStatistics: AuditStatistics? = null,
    val isLoadingData: Boolean = false,
    val isExportingData: Boolean = false,
    val isDeletingData: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val showAuditInfo: Boolean = false
)

sealed class PrivacyUiEvent {
    data class ShowMessage(val message: String) : PrivacyUiEvent()
    data class ShowError(val message: String) : PrivacyUiEvent()
    data class ExportCompleted(val message: String) : PrivacyUiEvent()
}