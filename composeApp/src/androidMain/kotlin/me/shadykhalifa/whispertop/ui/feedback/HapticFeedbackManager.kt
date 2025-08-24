package me.shadykhalifa.whispertop.ui.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.HapticFeedbackConstants
import android.view.View
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.ui.overlay.MicButtonState

/**
 * Manages haptic feedback patterns for different recording states and user interactions
 * 
 * Note: This class holds a context reference. Ensure cleanup() is called when done to prevent memory leaks.
 * 
 * @param context The context for accessing system services
 */
class HapticFeedbackManager(private val context: Context) {
    
    companion object {
        /**
         * Create a new HapticFeedbackManager instance
         * 
         * @param context The context for accessing system services
         * @return A new HapticFeedbackManager instance, or null if vibrator service is unavailable
         */
        fun create(context: Context): HapticFeedbackManager? {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            return if (vibrator?.hasVibrator() == true) {
                HapticFeedbackManager(context)
            } else {
                null
            }
        }
    }
    
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private var isCleanedUp = false
    
    /**
     * Predefined haptic feedback patterns for different recording states and user interactions
     * 
     * Each pattern is designed to provide distinct tactile feedback that helps users
     * understand the current state without looking at the screen.
     */
    sealed class FeedbackPattern {
        /** Double pulse to indicate recording has started */
        object StartRecording : FeedbackPattern()
        
        /** Single long pulse to indicate recording has stopped */
        object StopRecording : FeedbackPattern()
        
        /** Three short pulses to indicate recording is paused */
        object PauseRecording : FeedbackPattern()
        
        /** Ascending pattern to indicate recording has resumed */
        object ResumeRecording : FeedbackPattern()
        
        /** Gentle pulse to indicate processing has started */
        object ProcessingStart : FeedbackPattern()
        
        /** Quick double pulse to indicate processing is complete */
        object ProcessingComplete : FeedbackPattern()
        
        /** Triumphant ascending pattern for successful operations */
        object Success : FeedbackPattern()
        
        /** Strong pulse pattern to indicate error conditions */
        object Error : FeedbackPattern()
        
        /** Light single pulse for button press feedback */
        object ButtonPress : FeedbackPattern()
        
        /** Very light pulse for drag operations */
        object ButtonDrag : FeedbackPattern()
        
        /** Medium pulse when button snaps to position */
        object ButtonSnap : FeedbackPattern()
    }
    
    /**
     * Provides haptic feedback based on the pattern
     * 
     * @param pattern The haptic feedback pattern to perform
     * @param view Optional view for additional haptic feedback
     */
    fun performFeedback(pattern: FeedbackPattern, view: View? = null) {
        if (isCleanedUp || vibrator?.hasVibrator() != true) return
        
        when (pattern) {
            is FeedbackPattern.StartRecording -> performStartRecordingFeedback(view)
            is FeedbackPattern.StopRecording -> performStopRecordingFeedback(view)
            is FeedbackPattern.PauseRecording -> performPauseRecordingFeedback(view)
            is FeedbackPattern.ResumeRecording -> performResumeRecordingFeedback(view)
            is FeedbackPattern.ProcessingStart -> performProcessingStartFeedback(view)
            is FeedbackPattern.ProcessingComplete -> performProcessingCompleteFeedback(view)
            is FeedbackPattern.Success -> performSuccessFeedback(view)
            is FeedbackPattern.Error -> performErrorFeedback(view)
            is FeedbackPattern.ButtonPress -> performButtonPressFeedback(view)
            is FeedbackPattern.ButtonDrag -> performButtonDragFeedback(view)
            is FeedbackPattern.ButtonSnap -> performButtonSnapFeedback(view)
        }
    }
    
    /**
     * Provides haptic feedback based on recording state transitions
     */
    fun performFeedbackForStateTransition(
        fromState: RecordingState,
        toState: RecordingState,
        view: View? = null
    ) {
        when (toState) {
            is RecordingState.Recording -> {
                if (fromState is RecordingState.Idle) {
                    performFeedback(FeedbackPattern.StartRecording, view)
                } else {
                    performFeedback(FeedbackPattern.ResumeRecording, view)
                }
            }
            is RecordingState.Idle -> {
                if (fromState is RecordingState.Recording) {
                    performFeedback(FeedbackPattern.StopRecording, view)
                }
            }
            is RecordingState.Processing -> {
                performFeedback(FeedbackPattern.ProcessingStart, view)
            }
            is RecordingState.Success -> {
                performFeedback(FeedbackPattern.Success, view)
            }
            is RecordingState.Error -> {
                performFeedback(FeedbackPattern.Error, view)
            }
        }
    }
    
    /**
     * Provides haptic feedback based on MicButton state changes
     */
    fun performFeedbackForMicButtonState(state: MicButtonState, view: View? = null) {
        when (state) {
            MicButtonState.RECORDING -> performFeedback(FeedbackPattern.StartRecording, view)
            MicButtonState.PROCESSING -> performFeedback(FeedbackPattern.ProcessingStart, view)
            MicButtonState.IDLE -> performFeedback(FeedbackPattern.StopRecording, view)
        }
    }
    
    private fun performStartRecordingFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Double pulse to indicate start
                val pattern = VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 50, 100),
                    intArrayOf(0, 150, 0, 150),
                    -1
                )
                vibrator?.vibrate(pattern)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 100, 50, 100), -1)
            }
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: Exception) {
            // Ignore vibration errors - device might not support it
        }
    }
    
    private fun performStopRecordingFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Single long pulse to indicate stop
                vibrator?.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(150)
            }
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: Exception) {
            // Ignore vibration errors - device might not support it
        }
    }
    
    private fun performPauseRecordingFeedback(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Three short pulses to indicate pause
            val pattern = VibrationEffect.createWaveform(
                longArrayOf(0, 50, 30, 50, 30, 50),
                intArrayOf(0, 100, 0, 100, 0, 100),
                -1
            )
            vibrator?.vibrate(pattern)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 30, 50, 30, 50), -1)
        }
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
    
    private fun performResumeRecordingFeedback(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Ascending pattern to indicate resume
            val pattern = VibrationEffect.createWaveform(
                longArrayOf(0, 50, 30, 75, 30, 100),
                intArrayOf(0, 80, 0, 120, 0, 160),
                -1
            )
            vibrator?.vibrate(pattern)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 30, 75, 30, 100), -1)
        }
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
    
    private fun performProcessingStartFeedback(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Gentle pulse to indicate processing
            vibrator?.vibrate(VibrationEffect.createOneShot(75, 80))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(75)
        }
        view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }
    
    private fun performProcessingCompleteFeedback(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Quick double pulse to indicate processing complete
            val pattern = VibrationEffect.createWaveform(
                longArrayOf(0, 30, 20, 30),
                intArrayOf(0, 100, 0, 100),
                -1
            )
            vibrator?.vibrate(pattern)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 30, 20, 30), -1)
        }
        view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }
    
    private fun performSuccessFeedback(view: View?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Triumphant pattern for success
            val pattern = VibrationEffect.createWaveform(
                longArrayOf(0, 50, 30, 75, 30, 100),
                intArrayOf(0, 120, 0, 140, 0, 160),
                -1
            )
            vibrator?.vibrate(pattern)
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(longArrayOf(0, 50, 30, 75, 30, 100), -1)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            view?.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
        } else {
            view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        }
    }
    
    private fun performErrorFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Strong pulse pattern for error
                val pattern = VibrationEffect.createWaveform(
                    longArrayOf(0, 100, 100, 100, 100, 100),
                    intArrayOf(0, 200, 0, 200, 0, 200),
                    -1
                )
                vibrator?.vibrate(pattern)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 100, 100, 100, 100, 100), -1)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view?.performHapticFeedback(HapticFeedbackConstants.REJECT)
            } else {
                view?.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
        } catch (e: Exception) {
            // Ignore vibration errors - device might not support it
        }
    }
    
    private fun performButtonPressFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: SecurityException) {
            // Ignore vibration errors - app might not have VIBRATE permission
        } catch (e: Exception) {
            // Ignore other vibration errors - device might not support it
        }
    }
    
    private fun performButtonDragFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Light feedback for dragging
                vibrator?.vibrate(VibrationEffect.createOneShot(25, 50))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(25)
            }
            view?.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        } catch (e: SecurityException) {
            // Ignore vibration errors - app might not have VIBRATE permission
        } catch (e: Exception) {
            // Ignore other vibration errors - device might not support it
        }
    }
    
    private fun performButtonSnapFeedback(view: View?) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Snap feedback when button snaps to edge
                vibrator?.vibrate(VibrationEffect.createOneShot(80, 120))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(80)
            }
            view?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        } catch (e: SecurityException) {
            // Ignore vibration errors - app might not have VIBRATE permission
        } catch (e: Exception) {
            // Ignore other vibration errors - device might not support it
        }
    }
    
    /**
     * Check if haptic feedback is supported and enabled
     * 
     * @return true if haptic feedback is supported and manager is not cleaned up
     */
    fun isHapticFeedbackSupported(): Boolean {
        return !isCleanedUp && vibrator?.hasVibrator() == true
    }
    
    /**
     * Cleanup resources and cancel any ongoing vibrations
     * Call this method when the manager is no longer needed to prevent memory leaks
     */
    fun cleanup() {
        if (isCleanedUp) return
        
        try {
            // Cancel any ongoing vibrations
            vibrator?.cancel()
        } catch (e: Exception) {
            // Ignore exceptions during cleanup - vibrator might be unavailable
        }
        
        isCleanedUp = true
    }
}