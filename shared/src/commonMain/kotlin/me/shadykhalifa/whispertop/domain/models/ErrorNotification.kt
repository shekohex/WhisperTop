package me.shadykhalifa.whispertop.domain.models

import kotlinx.coroutines.flow.StateFlow

data class ErrorNotification(
    val id: String,
    val errorInfo: ErrorInfo,
    val timestamp: Long = System.currentTimeMillis(),
    val isShown: Boolean = false,
    val isDismissed: Boolean = false,
    val retryCount: Int = 0,
    val originalError: Throwable? = null
)

sealed class ErrorAction {
    data object Dismiss : ErrorAction()
    data object Retry : ErrorAction()
    data object OpenSettings : ErrorAction()
    data object GrantPermission : ErrorAction()
    data object CopyText : ErrorAction()
    data object StopRecording : ErrorAction()
    data class Custom(val action: String) : ErrorAction()
}

data class ErrorNotificationState(
    val activeNotifications: List<ErrorNotification> = emptyList(),
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN
)

enum class ConnectionStatus {
    CONNECTED,
    DISCONNECTED,
    CONNECTING,
    UNKNOWN
}

interface ErrorNotificationService {
    val notificationState: StateFlow<ErrorNotificationState>
    
    fun showError(error: Throwable, context: String? = null)
    fun handleAction(notificationId: String, action: ErrorAction)
    fun dismissNotification(notificationId: String)
    fun clearAllNotifications()
    fun updateConnectionStatus(status: ConnectionStatus)
}

class ErrorNotificationServiceImpl : ErrorNotificationService {
    
    private val _notificationState = kotlinx.coroutines.flow.MutableStateFlow(ErrorNotificationState())
    override val notificationState: StateFlow<ErrorNotificationState> = _notificationState
    
    override fun showError(error: Throwable, context: String?) {
        val errorInfo = ErrorClassifier.classifyError(error)
        val notification = ErrorNotification(
            id = generateNotificationId(),
            errorInfo = errorInfo,
            originalError = error
        )
        
        val currentState = _notificationState.value
        val updatedNotifications = currentState.activeNotifications + notification
        
        _notificationState.value = currentState.copy(
            activeNotifications = updatedNotifications.takeLast(5) // Keep max 5 notifications
        )
    }
    
    override fun handleAction(notificationId: String, action: ErrorAction) {
        val currentState = _notificationState.value
        val notification = currentState.activeNotifications.find { it.id == notificationId }
            ?: return
        
        when (action) {
            is ErrorAction.Dismiss -> dismissNotification(notificationId)
            is ErrorAction.Retry -> {
                // Mark as dismissed and let the caller handle retry logic
                dismissNotification(notificationId)
            }
            is ErrorAction.OpenSettings,
            is ErrorAction.GrantPermission,
            is ErrorAction.CopyText,
            is ErrorAction.StopRecording,
            is ErrorAction.Custom -> {
                // Mark as dismissed, action will be handled by the UI layer
                dismissNotification(notificationId)
            }
        }
    }
    
    override fun dismissNotification(notificationId: String) {
        val currentState = _notificationState.value
        val updatedNotifications = currentState.activeNotifications.filter { it.id != notificationId }
        
        _notificationState.value = currentState.copy(
            activeNotifications = updatedNotifications
        )
    }
    
    override fun clearAllNotifications() {
        val currentState = _notificationState.value
        _notificationState.value = currentState.copy(
            activeNotifications = emptyList()
        )
    }
    
    override fun updateConnectionStatus(status: ConnectionStatus) {
        val currentState = _notificationState.value
        _notificationState.value = currentState.copy(
            connectionStatus = status
        )
    }
    
    private fun generateNotificationId(): String {
        return "error_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }
}