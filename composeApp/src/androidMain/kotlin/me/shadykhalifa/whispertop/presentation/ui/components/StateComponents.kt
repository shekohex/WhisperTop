package me.shadykhalifa.whispertop.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    icon: ImageVector = Icons.Default.ErrorOutline,
    retryText: String = "Try Again"
) {
    var visible by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(600)) + 
                slideInVertically(
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 4 }
                ),
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Error state: $title - $message"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated error icon with pulsing effect
            val infiniteTransition = rememberInfiniteTransition(label = "errorPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "pulseScale"
            )
            
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .scale(pulseScale),
                tint = MaterialTheme.colorScheme.error
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Animated title with slide-in
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith 
                    fadeOut(animationSpec = tween(200))
                },
                label = "titleAnimation"
            ) { currentTitle ->
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Animated message
            AnimatedContent(
                targetState = message,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400, delayMillis = 200)) togetherWith 
                    fadeOut(animationSpec = tween(200))
                },
                label = "messageAnimation"
            ) { currentMessage ->
                Text(
                    text = currentMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated retry button with scale effect
            var isPressed by remember { mutableStateOf(false) }
            val buttonScale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessHigh
                ),
                label = "buttonScale"
            )
            
            Button(
                onClick = {
                    hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    onRetry()
                },
                modifier = Modifier
                    .scale(buttonScale),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = retryText,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun AnimatedEmptyState(
    title: String,
    message: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox
) {
    var visible by remember { mutableStateOf(false) }
    val hapticFeedback = LocalHapticFeedback.current
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(800)) + 
                slideInVertically(
                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                    initialOffsetY = { it / 6 }
                ),
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Empty state: $title - $message"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated floating icon
            val infiniteTransition = rememberInfiniteTransition(label = "floatingIcon")
            val floatingOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 12f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "floatingOffset"
            )
            
            val iconAlpha by infiniteTransition.animateFloat(
                initialValue = 0.7f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "iconAlpha"
            )
            
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .offset(y = floatingOffset.dp),
                shape = RoundedCornerShape(60.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                shadowElevation = 2.dp
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(48.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = iconAlpha)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated title with typewriter effect
            AnimatedContent(
                targetState = title,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500, delayMillis = 400)) + 
                    slideInVertically(
                        animationSpec = tween(500, delayMillis = 400),
                        initialOffsetY = { it / 4 }
                    ) togetherWith 
                    fadeOut(animationSpec = tween(200))
                },
                label = "titleAnimation"
            ) { currentTitle ->
                Text(
                    text = currentTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Animated message
            AnimatedContent(
                targetState = message,
                transitionSpec = {
                    fadeIn(animationSpec = tween(500, delayMillis = 600)) + 
                    slideInVertically(
                        animationSpec = tween(500, delayMillis = 600),
                        initialOffsetY = { it / 4 }
                    ) togetherWith 
                    fadeOut(animationSpec = tween(200))
                },
                label = "messageAnimation"
            ) { currentMessage ->
                Text(
                    text = currentMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                )
            }
            
            // Optional action button
            if (actionText != null && onAction != null) {
                Spacer(modifier = Modifier.height(32.dp))
                
                var isPressed by remember { mutableStateOf(false) }
                val buttonScale by animateFloatAsState(
                    targetValue = if (isPressed) 0.95f else 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "actionButtonScale"
                )
                
                OutlinedButton(
                    onClick = {
                        hapticFeedback.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                        onAction()
                    },
                    modifier = Modifier
                        .scale(buttonScale)
                        .animateContentSize(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = actionText,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedLoadingState(
    message: String = "Loading...",
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(400)),
        modifier = modifier
            .fillMaxSize()
            .semantics {
                contentDescription = "Loading: $message"
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated loading indicator with scale
            val infiniteTransition = rememberInfiniteTransition(label = "loadingAnimation")
            val loadingScale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "loadingScale"
            )
            
            CircularProgressIndicator(
                modifier = Modifier
                    .size(48.dp)
                    .scale(loadingScale),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            AnimatedContent(
                targetState = message,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith 
                    fadeOut(animationSpec = tween(200))
                },
                label = "loadingMessageAnimation"
            ) { currentMessage ->
                Text(
                    text = currentMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Specialized empty states for specific screens
@Composable
fun EmptyTranscriptionsState(
    onStartRecording: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        title = "No transcriptions yet",
        message = "Start recording to see your transcription history here. Your voice will be converted to text instantly!",
        actionText = "Start Recording",
        onAction = onStartRecording,
        icon = Icons.Default.RecordVoiceOver,
        modifier = modifier
    )
}

@Composable
fun NoSearchResultsState(
    searchQuery: String,
    onClearSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedEmptyState(
        title = "No results found",
        message = "We couldn't find any transcriptions matching \"$searchQuery\". Try adjusting your search terms.",
        actionText = "Clear Search",
        onAction = onClearSearch,
        icon = Icons.Default.SearchOff,
        modifier = modifier
    )
}

@Composable
fun NetworkErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedErrorState(
        title = "Connection Problem",
        message = "Please check your internet connection and try again. Make sure you're connected to Wi-Fi or mobile data.",
        onRetry = onRetry,
        icon = Icons.Default.CloudOff,
        retryText = "Retry Connection",
        modifier = modifier
    )
}

@Composable
fun ApiKeyErrorState(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedErrorState(
        title = "API Key Issue",
        message = "There seems to be a problem with your OpenAI API key. Please check your settings and ensure your key is valid.",
        onRetry = onRetry,
        icon = Icons.Default.Key,
        retryText = "Check Settings",
        modifier = modifier
    )
}