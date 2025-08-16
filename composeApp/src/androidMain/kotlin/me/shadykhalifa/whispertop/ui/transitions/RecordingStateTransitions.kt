package me.shadykhalifa.whispertop.ui.transitions

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.ui.overlay.MicButtonState
import me.shadykhalifa.whispertop.ui.overlay.components.AnimationConstants

/**
 * Material Motion-based state transitions for recording animations
 */
object RecordingStateTransitions {
    
    // Standard Material Motion durations
    const val DURATION_SHORT = 200
    const val DURATION_MEDIUM = 300
    const val DURATION_LONG = 500
    const val DURATION_EXTRA_LONG = 700
    
    /**
     * Animate color transitions between recording states
     */
    @Composable
    fun animateRecordingColor(
        targetState: MicButtonState,
        animationSpec: AnimationSpec<Color> = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EaseInOutCubic
        )
    ): Color {
        return animateColorAsState(
            targetValue = targetState.color,
            animationSpec = animationSpec,
            label = "recording_color"
        ).value
    }
    
    /**
     * Animate scale transitions for button press feedback
     */
    @Composable
    fun animateButtonScale(
        pressed: Boolean,
        animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = 0.6f,
            stiffness = 1200f
        )
    ): Float {
        return animateFloatAsState(
            targetValue = if (pressed) AnimationConstants.BUTTON_PRESSED_SCALE else AnimationConstants.BUTTON_NORMAL_SCALE,
            animationSpec = animationSpec,
            label = "button_scale"
        ).value
    }
    
    /**
     * Animate elevation for Material Design depth
     */
    @Composable
    fun animateButtonElevation(
        state: MicButtonState,
        pressed: Boolean = false,
        animationSpec: AnimationSpec<Float> = tween(
            durationMillis = DURATION_SHORT,
            easing = FastOutSlowInEasing
        )
    ): Float {
        val targetElevation = when {
            pressed -> 2f
            state == MicButtonState.RECORDING -> 8f
            state == MicButtonState.PROCESSING -> 6f
            else -> 4f
        }
        
        return animateFloatAsState(
            targetValue = targetElevation,
            animationSpec = animationSpec,
            label = "button_elevation"
        ).value
    }
    
    /**
     * Animate opacity transitions for smooth state changes
     */
    @Composable
    fun animateStateOpacity(
        visible: Boolean,
        animationSpec: AnimationSpec<Float> = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EaseInOut
        )
    ): Float {
        return animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = animationSpec,
            label = "state_opacity"
        ).value
    }
    
    /**
     * Animate icon rotation for processing state
     */
    @Composable
    fun animateProcessingRotation(
        isProcessing: Boolean,
        animationSpec: AnimationSpec<Float> = tween(
            durationMillis = DURATION_LONG,
            easing = EaseInOut
        )
    ): Float {
        return animateFloatAsState(
            targetValue = if (isProcessing) 360f else 0f,
            animationSpec = animationSpec,
            label = "processing_rotation"
        ).value
    }
    
    /**
     * Animate audio level visualization smoothly
     */
    @Composable
    fun animateAudioLevel(
        targetLevel: Float,
        animationSpec: AnimationSpec<Float> = tween(
            durationMillis = 150, // Fast response for audio
            easing = EaseInOut
        )
    ): Float {
        return animateFloatAsState(
            targetValue = targetLevel,
            animationSpec = animationSpec,
            label = "audio_level"
        ).value
    }
    
    /**
     * Animate success/error indicator appearance
     */
    @Composable
    fun animateIndicatorScale(
        visible: Boolean,
        animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = 0.5f,
            stiffness = 800f
        )
    ): Float {
        return animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = animationSpec,
            label = "indicator_scale"
        ).value
    }
    
    /**
     * Animate recording duration text appearance
     */
    @Composable
    fun animateDurationOpacity(
        recording: Boolean,
        animationSpec: AnimationSpec<Float> = tween(
            durationMillis = DURATION_MEDIUM,
            easing = EaseInOutCubic
        )
    ): Float {
        return animateFloatAsState(
            targetValue = if (recording) 1f else 0f,
            animationSpec = animationSpec,
            label = "duration_opacity"
        ).value
    }
    
    /**
     * Animate button position snapping
     */
    @Composable
    fun animatePositionX(
        targetX: Float,
        animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = 1000f
        )
    ): Float {
        return animateFloatAsState(
            targetValue = targetX,
            animationSpec = animationSpec,
            label = "position_x"
        ).value
    }
    
    @Composable
    fun animatePositionY(
        targetY: Float,
        animationSpec: AnimationSpec<Float> = spring(
            dampingRatio = 0.8f,
            stiffness = 1000f
        )
    ): Float {
        return animateFloatAsState(
            targetValue = targetY,
            animationSpec = animationSpec,
            label = "position_y"
        ).value
    }
    
    /**
     * Get transition duration based on state change
     */
    fun getTransitionDuration(fromState: RecordingState, toState: RecordingState): Int {
        return when {
            // Quick transitions for immediate feedback
            fromState is RecordingState.Idle && toState is RecordingState.Recording -> DURATION_SHORT
            fromState is RecordingState.Recording && toState is RecordingState.Idle -> DURATION_SHORT
            
            // Medium transitions for processing states
            fromState is RecordingState.Recording && toState is RecordingState.Processing -> DURATION_MEDIUM
            fromState is RecordingState.Processing && toState is RecordingState.Success -> DURATION_MEDIUM
            
            // Longer transitions for error states (more noticeable)
            toState is RecordingState.Error -> DURATION_LONG
            fromState is RecordingState.Error -> DURATION_MEDIUM
            
            // Default medium duration
            else -> DURATION_MEDIUM
        }
    }
    
    /**
     * Get appropriate easing for state transition
     */
    fun getTransitionEasing(fromState: RecordingState, toState: RecordingState) = when {
        // Snappy transitions for user actions
        fromState is RecordingState.Idle && toState is RecordingState.Recording -> FastOutSlowInEasing
        fromState is RecordingState.Recording && toState is RecordingState.Idle -> FastOutSlowInEasing
        
        // Smooth transitions for processing
        fromState is RecordingState.Recording && toState is RecordingState.Processing -> EaseInOutCubic
        fromState is RecordingState.Processing && toState is RecordingState.Success -> EaseInOut
        
        // Emphasized easing for error states
        toState is RecordingState.Error -> EaseInOut
        
        // Default smooth easing
        else -> EaseInOutCubic
    }
}