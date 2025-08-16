package me.shadykhalifa.whispertop.ui.accessibility

import android.content.Context
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import me.shadykhalifa.whispertop.R
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.ui.overlay.MicButtonState

/**
 * Manages accessibility announcements for recording state changes
 */
class AccessibilityAnnouncementManager(private val context: Context) {
    
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
    
    /**
     * Announces recording state changes for accessibility services
     * 
     * @param state The current recording state
     * @param view Optional view for accessibility events
     */
    fun announceStateChange(state: RecordingState, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        try {
        
        val announcement = when (state) {
            is RecordingState.Idle -> context.getString(R.string.accessibility_recording_idle)
            is RecordingState.Recording -> {
                val duration = formatDuration(state.duration)
                context.getString(R.string.accessibility_recording_started, duration)
            }
            is RecordingState.Processing -> {
                val progress = (state.progress * 100).toInt()
                context.getString(R.string.accessibility_processing, progress)
            }
            is RecordingState.Success -> {
                context.getString(R.string.accessibility_transcription_success, state.transcription.take(50))
            }
            is RecordingState.Error -> {
                val errorMessage = state.throwable.message ?: "Unknown error"
                if (state.retryable) {
                    context.getString(R.string.accessibility_error_retryable, errorMessage)
                } else {
                    context.getString(R.string.accessibility_error_not_retryable, errorMessage)
                }
            }
        }
        
            announceText(announcement, view)
        } catch (e: Exception) {
            android.util.Log.e("AccessibilityAnnouncement", "Failed to announce state change", e)
        }
    }
    
    /**
     * Announces MicButton state changes for accessibility services
     */
    fun announceMicButtonState(state: MicButtonState, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        val announcement = when (state) {
            MicButtonState.IDLE -> context.getString(R.string.accessibility_mic_button_idle)
            MicButtonState.RECORDING -> context.getString(R.string.accessibility_mic_button_recording)
            MicButtonState.PROCESSING -> context.getString(R.string.accessibility_mic_button_processing)
        }
        
        announceText(announcement, view)
    }
    
    /**
     * Announces audio level changes during recording
     */
    fun announceAudioLevel(level: Float, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        // Only announce significant level changes to avoid spam
        val levelPercent = (level * 100).toInt()
        if (levelPercent % 25 == 0 && levelPercent > 0) { // Announce at 25%, 50%, 75%, 100%
            val announcement = context.getString(R.string.accessibility_audio_level, levelPercent)
            announceText(announcement, view)
        }
    }
    
    /**
     * Announces recording duration updates
     */
    fun announceRecordingDuration(durationMs: Long, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        val duration = formatDuration(durationMs)
        val announcement = context.getString(R.string.accessibility_recording_duration, duration)
        announceText(announcement, view)
    }
    
    /**
     * Announces transcription progress
     */
    fun announceTranscriptionProgress(progress: Float, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        val progressPercent = (progress * 100).toInt()
        // Only announce at 25% intervals to avoid spam
        if (progressPercent % 25 == 0) {
            val announcement = context.getString(R.string.accessibility_transcription_progress, progressPercent)
            announceText(announcement, view)
        }
    }
    
    /**
     * Announces when text is successfully inserted
     */
    fun announceTextInserted(text: String, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        val truncatedText = text.take(100) // Limit length for announcement
        val announcement = context.getString(R.string.accessibility_text_inserted, truncatedText)
        announceText(announcement, view)
    }
    
    /**
     * Announces overlay position changes
     */
    fun announcePositionChange(x: Int, y: Int, screenWidth: Int, screenHeight: Int, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        
        val horizontalPosition = when {
            x < screenWidth * 0.33 -> context.getString(R.string.accessibility_position_left)
            x > screenWidth * 0.66 -> context.getString(R.string.accessibility_position_right)
            else -> context.getString(R.string.accessibility_position_center)
        }
        
        val verticalPosition = when {
            y < screenHeight * 0.33 -> context.getString(R.string.accessibility_position_top)
            y > screenHeight * 0.66 -> context.getString(R.string.accessibility_position_bottom)
            else -> context.getString(R.string.accessibility_position_middle)
        }
        
        val announcement = context.getString(R.string.accessibility_position_changed, verticalPosition, horizontalPosition)
        announceText(announcement, view)
    }
    
    /**
     * Announces custom messages
     */
    fun announceMessage(message: String, view: View? = null) {
        if (!isAccessibilityEnabled()) return
        announceText(message, view)
    }
    
    /**
     * Check if accessibility services are enabled
     */
    fun isAccessibilityEnabled(): Boolean {
        return try {
            accessibilityManager?.isEnabled == true
        } catch (e: Exception) {
            android.util.Log.e("AccessibilityAnnouncement", "Failed to check accessibility status", e)
            false
        }
    }
    
    /**
     * Check if touch exploration is enabled (TalkBack-like services)
     */
    fun isTouchExplorationEnabled(): Boolean {
        return accessibilityManager?.isTouchExplorationEnabled == true
    }
    
    private fun announceText(text: String, view: View?) {
        try {
            if (view != null) {
                // Use view's announceForAccessibility method if available
                view.announceForAccessibility(text)
            } else {
                // Fall back to accessibility event
                sendAccessibilityEvent(text)
            }
        } catch (e: Exception) {
            android.util.Log.e("AccessibilityAnnouncement", "Failed to announce text: $text", e)
        }
    }
    
    private fun sendAccessibilityEvent(text: String) {
        val event = AccessibilityEvent.obtain(AccessibilityEvent.TYPE_ANNOUNCEMENT)
        event.text.add(text)
        event.className = this::class.java.name
        event.packageName = context.packageName
        
        accessibilityManager?.sendAccessibilityEvent(event)
    }
    
    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        
        return if (minutes > 0) {
            context.getString(R.string.accessibility_duration_minutes_seconds, minutes, seconds)
        } else {
            context.getString(R.string.accessibility_duration_seconds, seconds)
        }
    }
}