package me.shadykhalifa.whispertop.domain.models

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class ErrorNotificationServiceTest {

    private val errorNotificationService = ErrorNotificationServiceImpl()

    @Test
    fun `showError should add notification to state`() = runTest {
        val error = TranscriptionError.ApiKeyMissing()
        
        errorNotificationService.showError(error)
        
        val state = errorNotificationService.notificationState.first()
        assertEquals(1, state.activeNotifications.size)
        
        val notification = state.activeNotifications.first()
        assertEquals("API Key Required", notification.errorInfo.title)
        assertEquals(error, notification.originalError)
        assertFalse(notification.isShown)
        assertFalse(notification.isDismissed)
        assertEquals(0, notification.retryCount)
    }

    @Test
    fun `showError should limit notifications to 5`() = runTest {
        // Add 7 notifications
        repeat(7) { index ->
            errorNotificationService.showError(RuntimeException("Error $index"))
        }
        
        val state = errorNotificationService.notificationState.first()
        assertEquals(5, state.activeNotifications.size)
        
        // Should keep the last 5 notifications
        val messages = state.activeNotifications.map { it.errorInfo.message }
        assertTrue(messages.any { it.contains("Error 2") })
        assertTrue(messages.any { it.contains("Error 6") })
        assertFalse(messages.any { it.contains("Error 0") })
        assertFalse(messages.any { it.contains("Error 1") })
    }

    @Test
    fun `dismissNotification should remove notification from state`() = runTest {
        val error = TranscriptionError.NetworkError(RuntimeException("Network error"))
        
        errorNotificationService.showError(error)
        val initialState = errorNotificationService.notificationState.first()
        assertEquals(1, initialState.activeNotifications.size)
        
        val notificationId = initialState.activeNotifications.first().id
        errorNotificationService.dismissNotification(notificationId)
        
        val finalState = errorNotificationService.notificationState.first()
        assertEquals(0, finalState.activeNotifications.size)
    }

    @Test
    fun `handleAction dismiss should remove notification`() = runTest {
        val error = TranscriptionError.RateLimitError()
        
        errorNotificationService.showError(error)
        val initialState = errorNotificationService.notificationState.first()
        val notificationId = initialState.activeNotifications.first().id
        
        errorNotificationService.handleAction(notificationId, ErrorAction.Dismiss)
        
        val finalState = errorNotificationService.notificationState.first()
        assertEquals(0, finalState.activeNotifications.size)
    }

    @Test
    fun `handleAction retry should remove notification`() = runTest {
        val error = TranscriptionError.NetworkError(RuntimeException("Network error"))
        
        errorNotificationService.showError(error)
        val initialState = errorNotificationService.notificationState.first()
        val notificationId = initialState.activeNotifications.first().id
        
        errorNotificationService.handleAction(notificationId, ErrorAction.Retry)
        
        val finalState = errorNotificationService.notificationState.first()
        assertEquals(0, finalState.activeNotifications.size)
    }

    @Test
    fun `clearAllNotifications should remove all notifications`() = runTest {
        // Add multiple notifications
        repeat(3) { index ->
            errorNotificationService.showError(RuntimeException("Error $index"))
        }
        
        val initialState = errorNotificationService.notificationState.first()
        assertEquals(3, initialState.activeNotifications.size)
        
        errorNotificationService.clearAllNotifications()
        
        val finalState = errorNotificationService.notificationState.first()
        assertEquals(0, finalState.activeNotifications.size)
    }

    @Test
    fun `updateConnectionStatus should update connection status in state`() = runTest {
        val initialState = errorNotificationService.notificationState.first()
        assertEquals(ConnectionStatus.UNKNOWN, initialState.connectionStatus)
        
        errorNotificationService.updateConnectionStatus(ConnectionStatus.CONNECTED)
        
        val updatedState = errorNotificationService.notificationState.first()
        assertEquals(ConnectionStatus.CONNECTED, updatedState.connectionStatus)
        
        errorNotificationService.updateConnectionStatus(ConnectionStatus.DISCONNECTED)
        
        val finalState = errorNotificationService.notificationState.first()
        assertEquals(ConnectionStatus.DISCONNECTED, finalState.connectionStatus)
    }

    @Test
    fun `handleAction should handle all action types without error`() = runTest {
        // Test each action type
        val actions = listOf(
            ErrorAction.OpenSettings,
            ErrorAction.GrantPermission,
            ErrorAction.CopyText,
            ErrorAction.StopRecording,
            ErrorAction.Custom("customAction")
        )
        
        actions.forEach { action ->
            // Clear all notifications before each test
            errorNotificationService.clearAllNotifications()
            
            // Add a new notification for each test
            val error = TranscriptionError.AccessibilityServiceNotEnabled()
            errorNotificationService.showError(error)
            
            val currentState = errorNotificationService.notificationState.first()
            assertEquals(1, currentState.activeNotifications.size)
            val currentNotificationId = currentState.activeNotifications.first().id
            
            // Handle action - should dismiss notification
            errorNotificationService.handleAction(currentNotificationId, action)
            
            val afterActionState = errorNotificationService.notificationState.first()
            assertEquals(0, afterActionState.activeNotifications.size)
        }
    }

    @Test
    fun `notification should have unique IDs`() = runTest {
        val error1 = TranscriptionError.ApiKeyMissing()
        val error2 = TranscriptionError.NetworkError(RuntimeException())
        
        errorNotificationService.showError(error1)
        errorNotificationService.showError(error2)
        
        val state = errorNotificationService.notificationState.first()
        assertEquals(2, state.activeNotifications.size)
        
        val id1 = state.activeNotifications[0].id
        val id2 = state.activeNotifications[1].id
        
        assertTrue(id1 != id2, "Notification IDs should be unique")
        assertTrue(id1.startsWith("error_"), "Notification ID should have correct prefix")
        assertTrue(id2.startsWith("error_"), "Notification ID should have correct prefix")
    }

    @Test
    fun `notification should have correct timestamp`() = runTest {
        val beforeTime = System.currentTimeMillis()
        
        errorNotificationService.showError(TranscriptionError.ApiKeyMissing())
        
        val afterTime = System.currentTimeMillis()
        val state = errorNotificationService.notificationState.first()
        val notification = state.activeNotifications.first()
        
        assertTrue(notification.timestamp >= beforeTime, "Timestamp should be after before time")
        assertTrue(notification.timestamp <= afterTime, "Timestamp should be before after time")
    }
}