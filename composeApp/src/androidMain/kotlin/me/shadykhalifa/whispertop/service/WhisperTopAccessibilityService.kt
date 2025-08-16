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
        return try {
            val focusedNode = findFocusedEditText()
            if (focusedNode != null) {
                insertTextDirectly(focusedNode, text)
            } else {
                insertTextViaClipboard(text)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting text", e)
            false
        }
    }

    private fun findFocusedEditText(): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        
        try {
            val focusedNode = findFocusedNode(rootNode)
            if (focusedNode != null && isEditableField(focusedNode)) {
                return focusedNode
            }
            
            return findEditableField(rootNode)
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
            val arguments = Bundle().apply {
                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            }
            
            val result = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            
            if (!result) {
                val pasteArguments = Bundle().apply {
                    putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                }
                node.performAction(AccessibilityNodeInfo.ACTION_PASTE, pasteArguments)
            } else {
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in direct text insertion", e)
            false
        } finally {
            node.recycle()
        }
    }

    private fun insertTextViaClipboard(text: String): Boolean {
        return try {
            val clipData = ClipData.newPlainText("WhisperTop", text)
            clipboardManager.setPrimaryClip(clipData)
            
            // Try to simulate paste with key events since GLOBAL_ACTION_PASTE might not be available
            val rootNode = rootInActiveWindow
            if (rootNode != null) {
                try {
                    val focusedNode = findFocusedEditText()
                    focusedNode?.performAction(AccessibilityNodeInfo.ACTION_PASTE) ?: false
                } finally {
                    rootNode.recycle()
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in clipboard text insertion", e)
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