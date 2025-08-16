package me.shadykhalifa.whispertop.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.ui.overlay.OverlayView
import org.koin.android.ext.android.inject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class OverlayService : Service() {
    
    companion object {
        private const val TAG = "OverlayService"
    }
    
    enum class OverlayState {
        IDLE, ACTIVE, ERROR
    }
    
    private val permissionHandler: PermissionHandler by inject()
    private val binder = OverlayServiceBinder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private var windowManager: WindowManager? = null
    private val overlayViews = ConcurrentHashMap<String, OverlayViewInfo>()
    private val isServiceRunning = AtomicBoolean(false)
    private var currentState = OverlayState.IDLE
    
    private val stateListeners = mutableSetOf<OverlayStateListener>()
    
    data class OverlayViewInfo(
        val view: OverlayView,
        val layoutParams: WindowManager.LayoutParams,
        val isVisible: Boolean = false
    )
    
    interface OverlayStateListener {
        fun onStateChanged(state: OverlayState)
        fun onOverlayAdded(overlayId: String)
        fun onOverlayRemoved(overlayId: String)
        fun onOverlayError(error: String)
    }
    
    inner class OverlayServiceBinder : Binder() {
        fun getService(): OverlayService = this@OverlayService
    }
    
    override fun onCreate() {
        super.onCreate()
        initializeWindowManager()
        isServiceRunning.set(true)
        setState(OverlayState.ACTIVE)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        cleanupAllOverlays()
        scope.cancel()
        isServiceRunning.set(false)
        setState(OverlayState.IDLE)
    }
    
    fun addStateListener(listener: OverlayStateListener) {
        stateListeners.add(listener)
    }
    
    fun removeStateListener(listener: OverlayStateListener) {
        stateListeners.remove(listener)
    }
    
    fun addOverlay(overlayId: String, overlayView: OverlayView, layoutParams: WindowManager.LayoutParams? = null): Boolean {
        if (!checkOverlayPermission()) {
            notifyError("Overlay permission not granted")
            return false
        }
        
        try {
            val params = layoutParams ?: createDefaultLayoutParams()
            val overlayInfo = OverlayViewInfo(overlayView, params)
            
            overlayViews[overlayId] = overlayInfo
            windowManager?.addView(overlayView, params)
            
            notifyOverlayAdded(overlayId)
            return true
            
        } catch (e: Exception) {
            overlayViews.remove(overlayId)
            notifyError("Failed to add overlay: ${e.message}")
            return false
        }
    }
    
    fun removeOverlay(overlayId: String): Boolean {
        val overlayInfo = overlayViews[overlayId] ?: return false
        
        try {
            windowManager?.removeView(overlayInfo.view)
            overlayViews.remove(overlayId)
            notifyOverlayRemoved(overlayId)
            return true
            
        } catch (e: Exception) {
            notifyError("Failed to remove overlay: ${e.message}")
            return false
        }
    }
    
    fun updateOverlayParams(overlayId: String, layoutParams: WindowManager.LayoutParams): Boolean {
        val overlayInfo = overlayViews[overlayId] ?: return false
        
        try {
            windowManager?.updateViewLayout(overlayInfo.view, layoutParams)
            overlayViews[overlayId] = overlayInfo.copy(layoutParams = layoutParams)
            return true
            
        } catch (e: Exception) {
            notifyError("Failed to update overlay params: ${e.message}")
            return false
        }
    }
    
    fun isOverlayVisible(overlayId: String): Boolean {
        return overlayViews[overlayId]?.isVisible == true
    }
    
    fun getActiveOverlays(): Set<String> {
        return overlayViews.keys.toSet()
    }
    
    fun getCurrentState(): OverlayState = currentState
    
    fun isServiceActive(): Boolean = isServiceRunning.get()
    
    private fun initializeWindowManager() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }
    
    private fun createDefaultLayoutParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 100
        }
    }
    
    private fun checkOverlayPermission(): Boolean {
        return permissionHandler.hasOverlayPermission()
    }
    
    private fun cleanupAllOverlays() {
        val overlayIds = overlayViews.keys.toList()
        for (overlayId in overlayIds) {
            removeOverlay(overlayId)
        }
    }
    
    private fun setState(state: OverlayState) {
        currentState = state
        stateListeners.forEach { it.onStateChanged(state) }
    }
    
    private fun notifyOverlayAdded(overlayId: String) {
        stateListeners.forEach { it.onOverlayAdded(overlayId) }
    }
    
    private fun notifyOverlayRemoved(overlayId: String) {
        stateListeners.forEach { it.onOverlayRemoved(overlayId) }
    }
    
    private fun notifyError(error: String) {
        setState(OverlayState.ERROR)
        stateListeners.forEach { it.onOverlayError(error) }
    }
}