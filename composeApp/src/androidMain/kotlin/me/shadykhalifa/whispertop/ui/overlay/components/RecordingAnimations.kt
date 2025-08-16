package me.shadykhalifa.whispertop.ui.overlay.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Collection of animated recording components for WhisperTop overlay
 * 
 * This file contains reusable Compose components for displaying various
 * recording states with smooth animations and Material Design principles.
 * 
 * @author WhisperTop Team
 * @since 1.0.0
 */

/**
 * A pulsing ring animation for indicating active recording state
 * 
 * Creates a circular ring that smoothly scales and fades in a pulsing motion,
 * providing visual feedback during audio recording.
 * 
 * @param size The diameter of the ring in DP
 * @param pulseColor The color of the pulsing ring
 * @param animationDurationMs Duration of one complete pulse cycle in milliseconds
 * @param scaleRange Range of scale values (min to max) for the pulsing effect
 * @param alphaRange Range of alpha values (max to min) for the fade effect
 * @param strokeWidth Width of the ring border in DP
 * @param modifier Compose modifier for styling and layout
 * 
 * @sample
 * ```kotlin
 * PulsingRecordingRing(
 *     size = 56.dp,
 *     pulseColor = Color.Red,
 *     animationDurationMs = 1000
 * )
 * ```
 */
@Composable
fun PulsingRecordingRing(
    size: Dp,
    pulseColor: Color = Color.White,
    animationDurationMs: Int = AnimationConstants.PULSING_ANIMATION_DURATION,
    scaleRange: Pair<Float, Float> = AnimationConstants.PULSING_SCALE_MIN to AnimationConstants.PULSING_SCALE_MAX,
    alphaRange: Pair<Float, Float> = AnimationConstants.PULSING_ALPHA_MAX to AnimationConstants.PULSING_ALPHA_MIN,
    strokeWidth: Dp = AnimationConstants.DEFAULT_STROKE_WIDTH_DP.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsing_ring")
    
    val scale by infiniteTransition.animateFloat(
        initialValue = scaleRange.first,
        targetValue = scaleRange.second,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = alphaRange.first,
        targetValue = alphaRange.second,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Canvas(
        modifier = modifier.size(size * scale)
    ) {
        drawCircle(
            color = pulseColor.copy(alpha = alpha),
            radius = this.size.minDimension / 2,
            style = Stroke(width = strokeWidth.toPx())
        )
    }
}

/**
 * A rotating spinner animation for indicating processing state
 * 
 * Displays a partial circular arc that continuously rotates to indicate
 * background processing or transcription in progress.
 * 
 * @param size The diameter of the spinner in DP
 * @param color The color of the spinning arc
 * @param strokeWidth Width of the arc stroke in DP
 * @param animationDurationMs Duration of one complete rotation in milliseconds
 * @param modifier Compose modifier for styling and layout
 * 
 * @sample
 * ```kotlin
 * ProcessingSpinner(
 *     size = 48.dp,
 *     color = Color.Blue,
 *     animationDurationMs = 1200
 * )
 * ```
 */
@Composable
fun ProcessingSpinner(
    size: Dp,
    color: Color = Color.White,
    strokeWidth: Dp = AnimationConstants.SPINNER_STROKE_WIDTH_DP.dp,
    animationDurationMs: Int = AnimationConstants.PROCESSING_SPINNER_DURATION,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "processing_spinner")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Canvas(
        modifier = modifier.size(size)
    ) {
        val radius = size.toPx() / 2
        val centerX = this.size.width / 2
        val centerY = this.size.height / 2
        
        // Draw partial circle arc for spinner effect
        drawArc(
            color = color,
            startAngle = rotation,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeWidth.toPx())
        )
    }
}

/**
 * Real-time audio level visualization with animated bars
 * 
 * Displays multiple vertical bars that animate based on the input audio level,
 * creating a visual representation of microphone input intensity.
 * 
 * @param audioLevel Current audio level from 0.0 (silent) to 1.0 (maximum)
 * @param size The overall size of the visualization in DP
 * @param barColor Color of the audio level bars
 * @param barCount Number of individual bars to display
 * @param barWidth Width of each individual bar in DP
 * @param barSpacing Spacing between bars in DP
 * @param animationDurationMs Duration for audio level changes to animate in milliseconds
 * @param modifier Compose modifier for styling and layout
 * 
 * @sample
 * ```kotlin
 * var audioLevel by remember { mutableStateOf(0f) }
 * AudioLevelVisualization(
 *     audioLevel = audioLevel,
 *     size = 32.dp,
 *     barColor = Color.Green,
 *     barCount = 6
 * )
 * ```
 */
@Composable
fun AudioLevelVisualization(
    audioLevel: Float, // 0.0 to 1.0
    size: Dp,
    barColor: Color = Color.White,
    barCount: Int = AnimationConstants.DEFAULT_AUDIO_BAR_COUNT,
    barWidth: Dp = AnimationConstants.THIN_STROKE_WIDTH_DP.dp,
    barSpacing: Dp = 1.dp,
    animationDurationMs: Int = AnimationConstants.AUDIO_LEVEL_ANIMATION_DURATION,
    modifier: Modifier = Modifier
) {
    // Performance optimization: Only animate if change is significant
    val targetLevel = remember(audioLevel) { 
        if (kotlin.math.abs(audioLevel - 0f) < AnimationConstants.MIN_AUDIO_LEVEL_CHANGE) 0f else audioLevel
    }
    
    val animatedLevel by animateFloatAsState(
        targetValue = targetLevel,
        animationSpec = tween(animationDurationMs),
        label = "audio_level"
    )
    
    Canvas(
        modifier = modifier.size(size)
    ) {
        // Cache expensive calculations
        val totalWidth = (barCount * barWidth.toPx()) + ((barCount - 1) * barSpacing.toPx())
        val startX = (this.size.width - totalWidth) / 2
        val centerY = this.size.height / 2
        val maxBarHeight = size.toPx() * AnimationConstants.AUDIO_BAR_HEIGHT_FACTOR
        
        // Only draw if audio level is significant enough
        if (animatedLevel > AnimationConstants.MIN_AUDIO_LEVEL_CHANGE) {
            repeat(barCount) { index ->
                val x = startX + (index * (barWidth.toPx() + barSpacing.toPx()))
                
                // Calculate bar height based on audio level and bar position
                val normalizedIndex = index.toFloat() / (barCount - 1)
                val barIntensity = kotlin.math.max(0f, animatedLevel - (normalizedIndex * AnimationConstants.AUDIO_BAR_INTENSITY_FACTOR))
                val barHeight = maxBarHeight * barIntensity
                
                if (barHeight > 2f) { // Skip drawing very small bars
                    drawRect(
                        color = barColor.copy(alpha = AnimationConstants.AUDIO_BAR_ALPHA_MIN + (barIntensity * AnimationConstants.AUDIO_BAR_ALPHA_RANGE)),
                        topLeft = androidx.compose.ui.geometry.Offset(x, centerY - barHeight / 2),
                        size = androidx.compose.ui.geometry.Size(barWidth.toPx(), barHeight)
                    )
                }
            }
        }
    }
}

/**
 * Animated checkmark for indicating successful completion
 * 
 * Draws a checkmark that animates from empty to fully drawn using a 
 * dash path effect, providing satisfying visual feedback for success states.
 * 
 * @param size The size of the checkmark container in DP
 * @param color Color of the checkmark stroke
 * @param strokeWidth Width of the checkmark lines in DP
 * @param animationDurationMs Duration for the checkmark to fully draw in milliseconds
 * @param modifier Compose modifier for styling and layout
 * 
 * @sample
 * ```kotlin
 * SuccessCheckmark(
 *     size = 24.dp,
 *     color = Color.Green,
 *     animationDurationMs = 300
 * )
 * ```
 */
@Composable
fun SuccessCheckmark(
    size: Dp,
    color: Color = Color.Green,
    strokeWidth: Dp = AnimationConstants.CHECKMARK_STROKE_WIDTH_DP.dp,
    animationDurationMs: Int = AnimationConstants.SUCCESS_CHECKMARK_DURATION,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(animationDurationMs),
        label = "progress"
    )
    
    Canvas(
        modifier = modifier.size(size)
    ) {
        val checkPath = androidx.compose.ui.graphics.Path().apply {
            val centerX = this@Canvas.size.width / 2
            val centerY = this@Canvas.size.height / 2
            val checkSize = kotlin.math.min(this@Canvas.size.width, this@Canvas.size.height) * AnimationConstants.AUDIO_VISUALIZATION_SIZE_FACTOR
            
            moveTo(centerX - checkSize, centerY)
            lineTo(centerX - checkSize * 0.2f, centerY + checkSize * 0.4f)
            lineTo(centerX + checkSize, centerY - checkSize * 0.4f)
        }
        
        drawPath(
            path = checkPath,
            color = color,
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    intervals = floatArrayOf(progress * 100f, (1f - progress) * 100f),
                    phase = 0f
                )
            )
        )
    }
}

/**
 * Pulsing circle animation for indicating error states
 * 
 * Creates a solid circle that pulses between different alpha values
 * to draw attention to error conditions.
 * 
 * @param size The diameter of the error pulse circle in DP
 * @param errorColor Color of the pulsing circle (typically red for errors)
 * @param animationDurationMs Duration of one complete pulse cycle in milliseconds
 * @param modifier Compose modifier for styling and layout
 * 
 * @sample
 * ```kotlin
 * ErrorPulse(
 *     size = 56.dp,
 *     errorColor = Color.Red,
 *     animationDurationMs = 500
 * )
 * ```
 */
@Composable
fun ErrorPulse(
    size: Dp,
    errorColor: Color = Color.Red,
    animationDurationMs: Int = AnimationConstants.ERROR_PULSE_DURATION,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "error_pulse")
    
    val alpha by infiniteTransition.animateFloat(
        initialValue = AnimationConstants.ERROR_ALPHA_MIN,
        targetValue = AnimationConstants.ERROR_ALPHA_MAX,
        animationSpec = infiniteRepeatable(
            animation = tween(animationDurationMs),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Box(
        modifier = modifier.size(size)
    ) {
        Canvas(
            modifier = Modifier.size(size)
        ) {
            drawCircle(
                color = errorColor.copy(alpha = alpha),
                radius = this.size.minDimension / 2
            )
        }
    }
}