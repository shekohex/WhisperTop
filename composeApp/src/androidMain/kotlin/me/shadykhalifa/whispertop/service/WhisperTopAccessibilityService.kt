package me.shadykhalifa.whispertop.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import me.shadykhalifa.whispertop.managers.ServiceRecoveryManager
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.utils.TextInsertionUtils
import org.koin.android.ext.android.inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import me.shadykhalifa.whispertop.utils.PrivacyUtils

class WhisperTopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "WhisperTopAccessibility"
        private var instance: WhisperTopAccessibilityService? = null
        
        fun getInstance(): WhisperTopAccessibilityService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var serviceRecoveryManager: ServiceRecoveryManager
    private val sessionMetricsRepository: SessionMetricsRepository by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val analyticsScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        serviceRecoveryManager = ServiceRecoveryManager(this)
        Log.d(TAG, "WhisperTop Accessibility Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        analyticsScope.cancel()
        instance = null
        Log.d(TAG, "WhisperTop Accessibility Service destroyed")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "WhisperTop Accessibility Service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't need to handle specific accessibility events for text insertion
        // This service is primarily used for inserting text into focused fields
    }

    override fun onInterrupt() {
        Log.d(TAG, "WhisperTop Accessibility Service interrupted")
    }

    fun insertText(text: String): Boolean {
        Log.d(TAG, "insertText called with text summary: ${PrivacyUtils.createLogSafeSummary(text)}")
        
        val insertionStartTime = System.currentTimeMillis()
        
        return try {
            Log.d(TAG, "Searching for focused edit text field...")
            val focusedNode = findFocusedEditText()
            val targetAppPackage = rootInActiveWindow?.packageName?.toString()
            
            val result = if (focusedNode != null) {
                Log.d(TAG, "Found focused node: ${focusedNode.className}, editable=${focusedNode.isEditable}, focused=${focusedNode.isFocused}")
                val insertionResult = insertTextDirectly(focusedNode, text)
                Log.d(TAG, "Direct text insertion result: $insertionResult")
                insertionResult
            } else {
                Log.w(TAG, "No focused edit text found, attempting clipboard insertion")
                val insertionResult = insertTextViaClipboard(text)
                Log.d(TAG, "Clipboard text insertion result: $insertionResult")
                insertionResult
            }
            
            // Track text insertion analytics
            trackTextInsertionAnalytics(
                text = text,
                success = result,
                targetAppPackage = targetAppPackage,
                insertionDurationMs = System.currentTimeMillis() - insertionStartTime
            )
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting text: ${e.message}", e)
            
            // Track failed insertion
            val targetAppPackage = rootInActiveWindow?.packageName?.toString()
            trackTextInsertionAnalytics(
                text = text,
                success = false,
                targetAppPackage = targetAppPackage,
                insertionDurationMs = System.currentTimeMillis() - insertionStartTime,
                errorMessage = e.message
            )
            
            false
        }
    }

    private fun findFocusedEditText(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.w(TAG, "rootInActiveWindow is null - no active window found")
            return null
        }
        
        Log.d(TAG, "Root window found: ${rootNode.className}, package: ${rootNode.packageName}")
        
        try {
            // First try to find the focused node
            val focusedNode = findFocusedNode(rootNode)
            if (focusedNode != null) {
                Log.d(TAG, "Found focused node: ${focusedNode.className}")
                if (isEditableField(focusedNode)) {
                    Log.d(TAG, "Focused node is editable - using it")
                    return focusedNode
                } else {
                    Log.d(TAG, "Focused node is not editable, searching for editable fields")
                    focusedNode.recycle()
                }
            } else {
                Log.d(TAG, "No focused node found, searching for any editable field")
            }
            
            // If no focused editable field, find any editable field
            val editableNode = findEditableField(rootNode)
            if (editableNode != null) {
                Log.d(TAG, "Found editable field: ${editableNode.className}")
            } else {
                Log.w(TAG, "No editable field found in the current window")
            }
            
            return editableNode
        } finally {
            rootNode.recycle()
        }
    }

    private fun findFocusedNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isFocused) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val focusedChild = findFocusedNode(child)
                if (focusedChild != null) {
                    return focusedChild
                }
            } finally {
                child.recycle()
            }
        }
        
        return null
    }

    private fun findEditableField(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (isEditableField(node)) {
            return node
        }
        
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                val editableChild = findEditableField(child)
                if (editableChild != null) {
                    return editableChild
                }
            } finally {
                child.recycle()
            }
        }
        
        return null
    }

    private fun isEditableField(node: AccessibilityNodeInfo): Boolean {
        return node.isEditable || 
               node.className?.toString()?.contains("EditText") == true ||
               node.className?.toString()?.contains("WebView") == true ||
               (node.isFocusable && node.isClickable)
    }

    private fun insertTextDirectly(node: AccessibilityNodeInfo, text: String): Boolean {
        return try {
            // Get current text and check for selection
            val existingText = node.text?.toString() ?: ""
            val hasSelection = node.textSelectionStart != node.textSelectionEnd
            
            Log.d(TAG, "Current text length: ${existingText.length}, has selection: $hasSelection")
            Log.d(TAG, "Selection: start=${node.textSelectionStart}, end=${node.textSelectionEnd}")
            
            val finalText = TextInsertionUtils.combineTextWithIntelligentSpacing(
                existingText = existingText,
                newText = text,
                hasSelection = hasSelection,
                selectionStart = node.textSelectionStart,
                selectionEnd = node.textSelectionEnd
            )
            
            Log.d(TAG, "Attempting direct text insertion with ACTION_SET_TEXT")
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, finalText)
            }
            
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            Log.d(TAG, "ACTION_SET_TEXT result: $result")
            
            if (!result) {
                Log.d(TAG, "ACTION_SET_TEXT failed, trying ACTION_PASTE")
                // Set the final text to clipboard and paste
                val clipData = ClipData.newPlainText("WhisperTop", finalText)
                clipboardManager.setPrimaryClip(clipData)
                
                val pasteResult = node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                Log.d(TAG, "ACTION_PASTE result: $pasteResult")
                pasteResult
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct text insertion: ${e.message}", e)
            false
        } finally {
            node.recycle()
        }
    }

    private fun insertTextViaClipboard(text: String): Boolean {
        return try {
            Log.d(TAG, "Attempting to find focused field for clipboard insertion")
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                try {
                    val focusedNode = findFocusedEditText()
                    if (focusedNode != null) {
                        try {
                            // Get current text and check for selection
                            val existingText = focusedNode.text?.toString() ?: ""
                            val hasSelection = focusedNode.textSelectionStart != focusedNode.textSelectionEnd
                            
                            val finalText = TextInsertionUtils.combineTextWithIntelligentSpacing(
                                existingText = existingText,
                                newText = text,
                                hasSelection = hasSelection,
                                selectionStart = focusedNode.textSelectionStart,
                                selectionEnd = focusedNode.textSelectionEnd
                            )
                            
                            Log.d(TAG, "Setting final text to clipboard")
                            val clipData = ClipData.newPlainText("WhisperTop", finalText)
                            clipboardManager.setPrimaryClip(clipData)
                            
                            // Use ACTION_SET_TEXT with the final text
                            val arguments = Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, finalText)
                            }
                            val setTextResult = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
                            
                            if (setTextResult) {
                                Log.d(TAG, "Clipboard text insertion via ACTION_SET_TEXT: $setTextResult")
                                setTextResult
                            } else {
                                Log.d(TAG, "ACTION_SET_TEXT failed, trying ACTION_PASTE")
                                val pasteResult = focusedNode.performAction(AccessibilityNodeInfo.ACTION_PASTE)
                                Log.d(TAG, "Clipboard paste result: $pasteResult")
                                pasteResult
                            }
                        } finally {
                            focusedNode.recycle()
                        }
                    } else {
                        Log.w(TAG, "No focused field found for paste action")
                        false
                    }
                } finally {
                    rootNode.recycle()
                }
            } else {
                Log.w(TAG, "No active window found for clipboard paste")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in clipboard text insertion: ${e.message}", e)
            false
        }
    }

    fun focusEditField(): Boolean {
        val focusedNode = findFocusedEditText()
        return if (focusedNode != null) {
            try {
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            } finally {
                focusedNode.recycle()
            }
        } else {
            false
        }
    }

    fun clearFocusedField(): Boolean {
        val focusedNode = findFocusedEditText()
        return if (focusedNode != null) {
            try {
                val arguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, "")
                }
                focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            } finally {
                focusedNode.recycle()
            }
        } else {
            false
        }
    }
    
    private fun trackTextInsertionAnalytics(
        text: String,
        success: Boolean,
        targetAppPackage: String?,
        insertionDurationMs: Long,
        errorMessage: String? = null
    ) {
        // Use dedicated analytics scope to prevent blocking text insertion
        analyticsScope.launch {
            try {
                val wordCount = countWords(text)
                val characterCount = text.length
                val typingSpeedWpm = calculateTypingSpeed(characterCount, insertionDurationMs)
                
                Log.d(TAG, "Text insertion analytics - Words: $wordCount, Characters: $characterCount, WPM: $typingSpeedWpm, App: $targetAppPackage, Success: $success, Sensitive: ${PrivacyUtils.containsSensitiveInfo(text)}")
                
                // Get current AudioRecordingService session to update metrics atomically
                AudioRecordingService.getInstance()?.let { service ->
                    service.getCurrentSessionId()?.let { sessionId ->
                        val speakingRate = service.getCurrentSessionMetrics()?.let { metrics ->
                            val audioDuration = metrics.audioRecordingDuration
                            if (audioDuration > 0) {
                                val minutes = audioDuration / 60000.0
                                if (minutes > 0 && wordCount > 0) {
                                    wordCount.toDouble() / minutes
                                } else 0.0
                            } else 0.0
                        } ?: 0.0
                        
                        // Use atomic transaction to update all text insertion data with timeout
                        kotlinx.coroutines.withTimeoutOrNull(5000) { // 5 second timeout for analytics
                            sessionMetricsRepository.updateSessionWithTextInsertionData(
                                sessionId = sessionId,
                                wordCount = wordCount,
                                characterCount = characterCount,
                                speakingRate = speakingRate,
                                transcriptionText = if (success) text else null,
                                transcriptionSuccess = success,
                                textInsertionSuccess = success,
                                targetAppPackage = targetAppPackage
                            )
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to track text insertion analytics: ${e.message}", e)
                // Analytics failure should not affect user experience
            }
        }
    }
    
    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        
        // Use regex to find word boundaries - handles multiple languages and edge cases
        return text.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .size
    }
    
    private fun calculateTypingSpeed(characterCount: Int, durationMs: Long): Double {
        if (durationMs <= 0) return 0.0
        
        // Standard WPM calculation: (characters / 5) / (time in minutes)
        // Dividing by 5 because average word length is approximately 5 characters
        val minutes = durationMs / 60000.0
        return if (minutes > 0) (characterCount / 5.0) / minutes else 0.0
    }
}