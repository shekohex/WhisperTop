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
import me.shadykhalifa.whispertop.utils.TextInsertionUtils

class WhisperTopAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "WhisperTopAccessibility"
        private var instance: WhisperTopAccessibilityService? = null
        
        fun getInstance(): WhisperTopAccessibilityService? = instance
        
        fun isServiceRunning(): Boolean = instance != null
    }

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var serviceRecoveryManager: ServiceRecoveryManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        serviceRecoveryManager = ServiceRecoveryManager(this)
        Log.d(TAG, "WhisperTop Accessibility Service created")
    }

    override fun onDestroy() {
        super.onDestroy()
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
        Log.d(TAG, "insertText called with text: '${text.take(50)}${if (text.length > 50) "..." else ""}' (${text.length} chars)")
        
        return try {
            Log.d(TAG, "Searching for focused edit text field...")
            val focusedNode = findFocusedEditText()
            
            if (focusedNode != null) {
                Log.d(TAG, "Found focused node: ${focusedNode.className}, editable=${focusedNode.isEditable}, focused=${focusedNode.isFocused}")
                val result = insertTextDirectly(focusedNode, text)
                Log.d(TAG, "Direct text insertion result: $result")
                result
            } else {
                Log.w(TAG, "No focused edit text found, attempting clipboard insertion")
                val result = insertTextViaClipboard(text)
                Log.d(TAG, "Clipboard text insertion result: $result")
                result
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting text: ${e.message}", e)
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
}