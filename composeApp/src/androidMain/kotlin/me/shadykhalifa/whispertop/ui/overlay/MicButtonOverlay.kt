package me.shadykhalifa.whispertop.ui.overlay

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import me.shadykhalifa.whispertop.ui.feedback.HapticFeedbackManager
import me.shadykhalifa.whispertop.ui.overlay.components.AudioLevelVisualization
import me.shadykhalifa.whispertop.ui.overlay.components.ErrorPulse
import me.shadykhalifa.whispertop.ui.overlay.components.ProcessingSpinner
import me.shadykhalifa.whispertop.ui.overlay.components.PulsingRecordingRing
import me.shadykhalifa.whispertop.ui.overlay.components.SuccessCheckmark
import me.shadykhalifa.whispertop.ui.overlay.components.AnimationConstants
import me.shadykhalifa.whispertop.ui.transitions.RecordingStateTransitions
import me.shadykhalifa.whispertop.ui.utils.PerformanceMonitor
import kotlin.math.max
import kotlin.math.min

class MicButtonOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : OverlayView(context, attrs, defStyleAttr), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val BUTTON_SIZE_DP = AnimationConstants.BUTTON_SIZE_DP
        private const val SNAP_THRESHOLD_DP = AnimationConstants.SNAP_THRESHOLD_DP
        private const val EDGE_MARGIN_DP = AnimationConstants.EDGE_MARGIN_DP
    }

    private var _currentState by mutableStateOf(MicButtonState.IDLE)
    private var _audioLevel by mutableFloatStateOf(0f)
    private var _showSuccessIndicator by mutableStateOf(false)
    private var _showErrorIndicator by mutableStateOf(false)
    
    private val hapticFeedbackManager = HapticFeedbackManager(context)
    private val micButtonListeners = mutableSetOf<MicButtonListener>()
    
    // Track delayed actions for proper cleanup
    private val delayedActions = mutableListOf<Runnable>()
    
    // Lifecycle management for Compose
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    
    override val lifecycle: Lifecycle = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry = savedStateRegistryController.savedStateRegistry
    
    interface MicButtonListener {
        fun onStateChanged(newState: MicButtonState)
        fun onMicButtonClicked()
        fun onPositionSnapped(x: Int, y: Int)
        fun onAudioLevelChanged(level: Float)
    }
    
    init {
        setDraggable(true)
        setupTouchHandling()
        setupLifecycle()
    }
    
    private fun setupLifecycle() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        
        // Set this view as the lifecycle owner for its view tree
        setViewTreeLifecycleOwner(this)
        setViewTreeSavedStateRegistryOwner(this)
    }
    
    fun addMicButtonListener(listener: MicButtonListener) {
        micButtonListeners.add(listener)
    }
    
    fun removeMicButtonListener(listener: MicButtonListener) {
        micButtonListeners.remove(listener)
    }
    
    fun setState(newState: MicButtonState) {
        if (_currentState != newState) {
            val previousState = _currentState
            _currentState = newState
            hapticFeedbackManager.performFeedbackForMicButtonState(newState, this)
            notifyStateChanged(newState)
            
            // Handle success/error indicators
            when (newState) {
                MicButtonState.IDLE -> {
                    if (previousState == MicButtonState.PROCESSING) {
                        showSuccessIndicator()
                    }
                    _audioLevel = 0f
                }
                MicButtonState.RECORDING -> {
                    _showSuccessIndicator = false
                    _showErrorIndicator = false
                }
                MicButtonState.PROCESSING -> {
                    _audioLevel = 0f
                }
            }
        }
    }
    
    fun getCurrentState(): MicButtonState = _currentState
    
    fun setAudioLevel(level: Float) {
        val newLevel = level.coerceIn(0f, 1f)
        
        // Performance optimization: Only update if change is significant
        if (kotlin.math.abs(newLevel - _audioLevel) >= AnimationConstants.MIN_AUDIO_LEVEL_CHANGE) {
            _audioLevel = newLevel
            notifyAudioLevelChanged(_audioLevel)
        }
    }
    
    fun showSuccessIndicator() {
        _showSuccessIndicator = true
        _showErrorIndicator = false
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.Success, this)
        
        // Hide after delay with proper cleanup tracking
        val hideAction: Runnable = object : Runnable {
            override fun run() {
                _showSuccessIndicator = false
                delayedActions.remove(this)
            }
        }
        delayedActions.add(hideAction)
        postDelayed(hideAction, 2000)
    }
    
    fun showErrorIndicator() {
        _showErrorIndicator = true
        _showSuccessIndicator = false
        hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.Error, this)
        
        // Hide after delay with proper cleanup tracking
        val hideAction: Runnable = object : Runnable {
            override fun run() {
                _showErrorIndicator = false
                delayedActions.remove(this)
            }
        }
        delayedActions.add(hideAction)
        postDelayed(hideAction, 3000)
    }
    
    override fun createCollapsedView(): View {
        return ComposeView(context).apply {
            // Ensure lifecycle is started before setting content
            lifecycleRegistry.currentState = Lifecycle.State.STARTED
            
            setContent {
                MicButtonContent(
                    state = _currentState,
                    size = getButtonSize(),
                    audioLevel = _audioLevel,
                    showSuccessIndicator = _showSuccessIndicator,
                    showErrorIndicator = _showErrorIndicator
                )
                
                // Monitor performance - can be enabled for debugging
                // androidx.compose.runtime.LaunchedEffect(Unit) {
                //     PerformanceMonitor.MemoryMonitor.logMemoryUsage("MicButtonOverlay")
                // }
            }
        }
    }
    
    override fun createExpandedView(): View {
        return createCollapsedView()
    }
    
    @Composable
    private fun MicButtonContent(
        state: MicButtonState,
        size: Int,
        audioLevel: Float,
        showSuccessIndicator: Boolean,
        showErrorIndicator: Boolean
    ) {
        // Use Material Motion transitions
        val animatedColor = RecordingStateTransitions.animateRecordingColor(state)
        val buttonScale = RecordingStateTransitions.animateButtonScale(pressed = false)
        val successScale = RecordingStateTransitions.animateIndicatorScale(showSuccessIndicator)
        val errorOpacity = RecordingStateTransitions.animateStateOpacity(showErrorIndicator)
        val animatedAudioLevel = RecordingStateTransitions.animateAudioLevel(audioLevel)
        
        val buttonColor = when {
            showErrorIndicator -> Color.Red.copy(alpha = errorOpacity)
            showSuccessIndicator -> Color.Green
            else -> animatedColor
        }
        
        Box(
            modifier = Modifier
                .size((size * buttonScale).dp)
                .clip(CircleShape)
                .background(buttonColor),
            contentAlignment = Alignment.Center
        ) {
            // Background animations
            when {
                showErrorIndicator -> {
                    ErrorPulse(
                        size = size.dp,
                        errorColor = Color.Red.copy(alpha = 0.3f)
                    )
                }
                state == MicButtonState.RECORDING -> {
                    PulsingRecordingRing(
                        size = size.dp,
                        pulseColor = Color.White
                    )
                }
                state == MicButtonState.PROCESSING -> {
                    ProcessingSpinner(
                        size = (size * AnimationConstants.PROCESSING_SPINNER_SIZE_FACTOR).dp,
                        color = Color.White
                    )
                }
            }
            
            // Audio level visualization for recording state
            if (state == MicButtonState.RECORDING && animatedAudioLevel > 0f) {
                AudioLevelVisualization(
                    audioLevel = animatedAudioLevel,
                    size = (size * AnimationConstants.AUDIO_VISUALIZATION_SIZE_FACTOR).dp,
                    barColor = Color.White,
                    barCount = 6,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
            
            // Success indicator with Material Motion scaling
            AnimatedVisibility(
                visible = showSuccessIndicator,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                SuccessCheckmark(
                    size = (size * AnimationConstants.SUCCESS_CHECKMARK_SIZE_FACTOR * successScale).dp,
                    color = Color.White
                )
            }
            
            // Main icon (hidden when showing success indicator)
            AnimatedVisibility(
                visible = !showSuccessIndicator,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                val iconVector = when {
                    showErrorIndicator -> Icons.Default.Error
                    else -> Icons.Default.Mic
                }
                
                Icon(
                    imageVector = iconVector,
                    contentDescription = state.description,
                    tint = Color.White,
                    modifier = Modifier.size((size * AnimationConstants.ICON_SIZE_FACTOR).dp)
                )
            }
        }
    }
    
    private fun getButtonSize(): Int {
        val metrics = context.resources.displayMetrics
        val density = metrics.density
        return when {
            density >= 3.0 -> (BUTTON_SIZE_DP * 1.2).toInt()
            density >= 2.0 -> BUTTON_SIZE_DP
            else -> (BUTTON_SIZE_DP * 0.8).toInt()
        }
    }
    
    private fun setupTouchHandling() {
        addOverlayListener(object : OverlayViewListener {
            override fun onExpandStateChanged(isExpanded: Boolean) {}
            
            override fun onPositionChanged(x: Int, y: Int) {
                handlePositionChange(x, y)
            }
            
            override fun onOverlayClicked() {
                hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.ButtonPress, this@MicButtonOverlay)
                notifyMicButtonClicked()
            }
            
            override fun onOverlayDismissed() {}
        })
    }
    
    private fun handlePositionChange(x: Int, y: Int) {
        val metrics = context.resources.displayMetrics
        val screenWidth = metrics.widthPixels
        val screenHeight = metrics.heightPixels
        
        val buttonSize = getButtonSize() * metrics.density
        val snapThreshold = SNAP_THRESHOLD_DP * metrics.density
        val edgeMargin = EDGE_MARGIN_DP * metrics.density
        
        var newX = x
        var newY = y
        
        if (x < snapThreshold) {
            newX = edgeMargin.toInt()
        } else if (x > screenWidth - buttonSize - snapThreshold) {
            newX = (screenWidth - buttonSize - edgeMargin).toInt()
        }
        
        newY = max(edgeMargin.toInt(), min(newY, (screenHeight - buttonSize - edgeMargin).toInt()))
        
        if (newX != x || newY != y) {
            updatePosition(newX, newY)
            hapticFeedbackManager.performFeedback(HapticFeedbackManager.FeedbackPattern.ButtonSnap, this)
            notifyPositionSnapped(newX, newY)
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val (currentX, currentY) = getCurrentPosition()
        handlePositionChange(currentX, currentY)
        updateViewForState()
    }
    
    private fun notifyStateChanged(newState: MicButtonState) {
        micButtonListeners.forEach { it.onStateChanged(newState) }
    }
    
    private fun notifyMicButtonClicked() {
        micButtonListeners.forEach { it.onMicButtonClicked() }
    }
    
    private fun notifyPositionSnapped(x: Int, y: Int) {
        micButtonListeners.forEach { it.onPositionSnapped(x, y) }
    }
    
    private fun notifyAudioLevelChanged(level: Float) {
        micButtonListeners.forEach { it.onAudioLevelChanged(level) }
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }
    
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        cleanup()
    }
    
    /**
     * Clean up resources and prevent memory leaks
     */
    fun cleanup() {
        // Cancel all pending delayed actions
        delayedActions.forEach { runnable ->
            removeCallbacks(runnable)
        }
        delayedActions.clear()
        
        // Clear listeners to prevent potential memory leaks
        micButtonListeners.clear()
        
        // Cleanup haptic feedback manager
        hapticFeedbackManager.cleanup()
        
        // Reset state
        _showSuccessIndicator = false
        _showErrorIndicator = false
        _audioLevel = 0f
    }
}