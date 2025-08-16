package me.shadykhalifa.whispertop.ui.overlay

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import kotlin.math.abs

abstract class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {
    
    companion object {
        private const val TAG = "OverlayView"
        
        fun createLayoutParams(
            width: Int = WindowManager.LayoutParams.WRAP_CONTENT,
            height: Int = WindowManager.LayoutParams.WRAP_CONTENT,
            x: Int = 0,
            y: Int = 100,
            gravity: Int = Gravity.TOP or Gravity.START,
            flags: Int = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        ): WindowManager.LayoutParams {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }
            
            return WindowManager.LayoutParams(
                width,
                height,
                type,
                flags,
                PixelFormat.TRANSLUCENT
            ).apply {
                this.gravity = gravity
                this.x = x
                this.y = y
            }
        }
    }
    
    private var isDraggable = false
    private var isExpanded = false
    private var isDragging = false
    
    private var initialX = 0
    private var initialY = 0
    private var initialTouchX = 0f
    private var initialTouchY = 0f
    
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private var layoutParams: WindowManager.LayoutParams? = null
    private var windowManager: WindowManager? = null
    
    private val overlayListeners = mutableSetOf<OverlayViewListener>()
    
    interface OverlayViewListener {
        fun onExpandStateChanged(isExpanded: Boolean)
        fun onPositionChanged(x: Int, y: Int)
        fun onOverlayClicked()
        fun onOverlayDismissed()
    }
    
    init {
        setupView()
    }
    
    @CallSuper
    protected open fun setupView() {
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setOnTouchListener(::handleTouch)
    }
    
    fun addOverlayListener(listener: OverlayViewListener) {
        overlayListeners.add(listener)
    }
    
    fun removeOverlayListener(listener: OverlayViewListener) {
        overlayListeners.remove(listener)
    }
    
    fun setDraggable(draggable: Boolean) {
        isDraggable = draggable
    }
    
    fun isDraggable(): Boolean = isDraggable
    
    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            onExpandStateChanged()
            notifyExpandStateChanged()
        }
    }
    
    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            onExpandStateChanged()
            notifyExpandStateChanged()
        }
    }
    
    fun toggleExpanded() {
        if (isExpanded) collapse() else expand()
    }
    
    fun isExpanded(): Boolean = isExpanded
    
    fun setLayoutParams(params: WindowManager.LayoutParams) {
        layoutParams = params
    }
    
    fun updatePosition(x: Int, y: Int) {
        layoutParams?.let { params ->
            params.x = x
            params.y = y
            windowManager?.updateViewLayout(this, params)
            notifyPositionChanged(x, y)
        }
    }
    
    fun getCurrentPosition(): Pair<Int, Int> {
        return layoutParams?.let { Pair(it.x, it.y) } ?: Pair(0, 0)
    }
    
    fun dismiss() {
        notifyOverlayDismissed()
    }
    
    private fun handleTouch(view: View, event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                if (isDraggable) {
                    initialX = layoutParams?.x ?: 0
                    initialY = layoutParams?.y ?: 0
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                }
                return true
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (isDraggable && !isDragging) {
                    val deltaX = abs(event.rawX - initialTouchX)
                    val deltaY = abs(event.rawY - initialTouchY)
                    
                    if (deltaX > touchSlop || deltaY > touchSlop) {
                        isDragging = true
                    }
                }
                
                if (isDraggable && isDragging) {
                    val newX = initialX + (event.rawX - initialTouchX).toInt()
                    val newY = initialY + (event.rawY - initialTouchY).toInt()
                    updatePosition(newX, newY)
                }
                return true
            }
            
            MotionEvent.ACTION_UP -> {
                if (isDraggable) {
                    if (!isDragging) {
                        performClick()
                        notifyOverlayClicked()
                    }
                    isDragging = false
                } else {
                    performClick()
                    notifyOverlayClicked()
                }
                return true
            }
        }
        return false
    }
    
    @CallSuper
    protected open fun onExpandStateChanged() {
        // Override in subclasses to handle expand/collapse animations or UI changes
    }
    
    protected abstract fun createCollapsedView(): View
    
    protected abstract fun createExpandedView(): View
    
    @CallSuper
    protected open fun updateViewForState() {
        removeAllViews()
        val view = if (isExpanded) createExpandedView() else createCollapsedView()
        addView(view)
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateViewForState()
    }
    
    private fun notifyExpandStateChanged() {
        overlayListeners.forEach { it.onExpandStateChanged(isExpanded) }
    }
    
    private fun notifyPositionChanged(x: Int, y: Int) {
        overlayListeners.forEach { it.onPositionChanged(x, y) }
    }
    
    private fun notifyOverlayClicked() {
        overlayListeners.forEach { it.onOverlayClicked() }
    }
    
    private fun notifyOverlayDismissed() {
        overlayListeners.forEach { it.onOverlayDismissed() }
    }
}