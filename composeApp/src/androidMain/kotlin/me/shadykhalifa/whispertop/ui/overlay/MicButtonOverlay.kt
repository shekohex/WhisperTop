package me.shadykhalifa.whispertop.ui.overlay

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class MicButtonOverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : OverlayView(context, attrs, defStyleAttr) {

    companion object {
        private const val BUTTON_SIZE_DP = 56
        private const val SNAP_THRESHOLD_DP = 48
        private const val EDGE_MARGIN_DP = 16
    }

    private var _currentState by mutableStateOf(MicButtonState.IDLE)
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    private val micButtonListeners = mutableSetOf<MicButtonListener>()
    
    interface MicButtonListener {
        fun onStateChanged(newState: MicButtonState)
        fun onMicButtonClicked()
        fun onPositionSnapped(x: Int, y: Int)
    }
    
    init {
        setDraggable(true)
        setupTouchHandling()
    }
    
    fun addMicButtonListener(listener: MicButtonListener) {
        micButtonListeners.add(listener)
    }
    
    fun removeMicButtonListener(listener: MicButtonListener) {
        micButtonListeners.remove(listener)
    }
    
    fun setState(newState: MicButtonState) {
        if (_currentState != newState) {
            _currentState = newState
            performHapticFeedback()
            notifyStateChanged(newState)
        }
    }
    
    fun getCurrentState(): MicButtonState = _currentState
    
    override fun createCollapsedView(): View {
        return ComposeView(context).apply {
            setContent {
                MicButtonContent(
                    state = _currentState,
                    size = getButtonSize()
                )
            }
        }
    }
    
    override fun createExpandedView(): View {
        return createCollapsedView()
    }
    
    @Composable
    private fun MicButtonContent(
        state: MicButtonState,
        size: Int
    ) {
        Box(
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
                .background(state.color),
            contentAlignment = Alignment.Center
        ) {
            if (state == MicButtonState.RECORDING) {
                PulsingRing(size = size)
            }
            
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = state.description,
                tint = Color.White,
                modifier = Modifier.size((size * 0.6f).dp)
            )
        }
    }
    
    @Composable
    private fun PulsingRing(size: Int) {
        val infiniteTransition = rememberInfiniteTransition(label = "pulsing")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.2f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Canvas(
            modifier = Modifier.size((size * scale).dp)
        ) {
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = this.size.minDimension / 2,
                style = Stroke(width = 4.dp.toPx())
            )
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
                performHapticFeedback()
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
            notifyPositionSnapped(newX, newY)
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        val (currentX, currentY) = getCurrentPosition()
        handlePositionChange(currentX, currentY)
        updateViewForState()
    }
    
    private fun performHapticFeedback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(50)
        }
        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
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
}