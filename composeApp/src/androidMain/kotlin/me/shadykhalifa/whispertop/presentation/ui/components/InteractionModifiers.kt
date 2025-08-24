package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Adds a bounce animation when the composable is pressed
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.95f,
    hapticFeedback: Boolean = true
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed.value = true
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                is PressInteraction.Release -> {
                    isPressed.value = false
                }
                is PressInteraction.Cancel -> {
                    isPressed.value = false
                }
            }
        }
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed.value) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "bounceScale"
    )
    
    scale(animatedScale)
}

/**
 * Adds a spring-based scaling animation with elevation change
 */
fun Modifier.springClick(
    pressedScale: Float = 0.96f,
    pressedElevation: Float = 8f,
    normalElevation: Float = 4f,
    hapticFeedback: Boolean = true
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed.value = true
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                is PressInteraction.Release -> {
                    isPressed.value = false
                }
                is PressInteraction.Cancel -> {
                    isPressed.value = false
                }
            }
        }
    }
    
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed.value) pressedScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springScale"
    )
    
    val animatedElevation by animateFloatAsState(
        targetValue = if (isPressed.value) pressedElevation else normalElevation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "springElevation"
    )
    
    graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        shadowElevation = animatedElevation
    }
}

/**
 * Adds a press ripple effect with rotation
 */
fun Modifier.pressRotation(
    maxRotation: Float = 1f,
    hapticFeedback: Boolean = true
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed.value = true
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                is PressInteraction.Release -> {
                    isPressed.value = false
                }
                is PressInteraction.Cancel -> {
                    isPressed.value = false
                }
            }
        }
    }
    
    val animatedRotation by animateFloatAsState(
        targetValue = if (isPressed.value) maxRotation else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "pressRotation"
    )
    
    graphicsLayer {
        rotationZ = animatedRotation
    }
}

/**
 * Adds a shimmer effect when pressed
 */
fun Modifier.shimmerClick(
    duration: Int = 1000,
    hapticFeedback: Boolean = true
): Modifier = composed {
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed = remember { mutableStateOf(false) }
    
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collectLatest { interaction ->
            when (interaction) {
                is PressInteraction.Press -> {
                    isPressed.value = true
                    if (hapticFeedback) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                }
                is PressInteraction.Release -> {
                    isPressed.value = false
                }
                is PressInteraction.Cancel -> {
                    isPressed.value = false
                }
            }
        }
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val shimmerAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmerAlpha"
    )
    
    if (isPressed.value) {
        graphicsLayer {
            alpha = shimmerAlpha
        }
    } else {
        this
    }
}

/**
 * Adds a pulsing effect when activated
 */
fun Modifier.pulseEffect(
    isActive: Boolean,
    minScale: Float = 1f,
    maxScale: Float = 1.05f,
    duration: Int = 1000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = minScale,
        targetValue = maxScale,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    
    if (isActive) {
        graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    } else {
        this
    }
}

/**
 * Adds a breathing animation effect
 */
fun Modifier.breathingAnimation(
    isActive: Boolean = true,
    minAlpha: Float = 0.7f,
    maxAlpha: Float = 1f,
    duration: Int = 2000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "breathing")
    val alpha by infiniteTransition.animateFloat(
        initialValue = minAlpha,
        targetValue = maxAlpha,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingAlpha"
    )
    
    if (isActive) {
        graphicsLayer {
            this.alpha = alpha
        }
    } else {
        this
    }
}

/**
 * Adds a subtle hover effect for elevated surfaces
 */
fun Modifier.hoverEffect(
    isHovered: Boolean,
    hoverScale: Float = 1.02f,
    hoverElevation: Float = 6f,
    normalElevation: Float = 2f
): Modifier = composed {
    val animatedScale by animateFloatAsState(
        targetValue = if (isHovered) hoverScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hoverScale"
    )
    
    val animatedElevation by animateFloatAsState(
        targetValue = if (isHovered) hoverElevation else normalElevation,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "hoverElevation"
    )
    
    graphicsLayer {
        scaleX = animatedScale
        scaleY = animatedScale
        shadowElevation = animatedElevation
    }
}

/**
 * Adds a success animation with color change
 */
fun Modifier.successAnimation(
    isSuccess: Boolean,
    successScale: Float = 1.1f,
    duration: Int = 300
): Modifier = composed {
    val animatedScale by animateFloatAsState(
        targetValue = if (isSuccess) successScale else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "successScale"
    )
    
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            // Reset success state after animation
            kotlinx.coroutines.delay(duration.toLong())
        }
    }
    
    scale(animatedScale)
}

/**
 * Adds a loading spinner rotation
 */
fun Modifier.loadingRotation(
    isLoading: Boolean,
    duration: Int = 1000
): Modifier = composed {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(duration, easing = LinearEasing)
        ),
        label = "loadingRotation"
    )
    
    if (isLoading) {
        graphicsLayer {
            rotationZ = rotation
        }
    } else {
        this
    }
}