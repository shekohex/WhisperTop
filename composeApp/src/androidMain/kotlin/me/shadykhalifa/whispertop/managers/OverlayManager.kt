package me.shadykhalifa.whispertop.managers

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.WindowManager
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.service.OverlayService
import me.shadykhalifa.whispertop.ui.overlay.OverlayView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.util.concurrent.atomic.AtomicBoolean

class OverlayManager : KoinComponent, DefaultLifecycleObserver {
    
    private val context: Context by inject()
    private val permissionHandler: PermissionHandler by inject()
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val isServiceBound = AtomicBoolean(false)
    private var overlayService: OverlayService? = null
    
    private val _serviceState = MutableStateFlow(ServiceState.DISCONNECTED)
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()
    
    private val _overlayState = MutableStateFlow(OverlayState.IDLE)
    val overlayState: StateFlow<OverlayState> = _overlayState.asStateFlow()
    
    private val overlayListeners = mutableSetOf<OverlayManagerListener>()
    
    enum class ServiceState {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }
    
    enum class OverlayState {
        IDLE, ACTIVE, ERROR
    }
    
    interface OverlayManagerListener {
        fun onServiceStateChanged(state: ServiceState)
        fun onOverlayStateChanged(state: OverlayState)
        fun onOverlayAdded(overlayId: String)
        fun onOverlayRemoved(overlayId: String)
        fun onOverlayError(error: String)
    }
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? OverlayService.OverlayServiceBinder
            overlayService = binder?.getService()
            isServiceBound.set(true)
            
            overlayService?.addStateListener(overlayStateListener)
            _serviceState.value = ServiceState.CONNECTED
            notifyServiceStateChanged(ServiceState.CONNECTED)
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            overlayService?.removeStateListener(overlayStateListener)
            overlayService = null
            isServiceBound.set(false)
            _serviceState.value = ServiceState.DISCONNECTED
            notifyServiceStateChanged(ServiceState.DISCONNECTED)
        }
    }
    
    private val overlayStateListener = object : OverlayService.OverlayStateListener {
        override fun onStateChanged(state: OverlayService.OverlayState) {
            val managerState = when (state) {
                OverlayService.OverlayState.IDLE -> OverlayState.IDLE
                OverlayService.OverlayState.ACTIVE -> OverlayState.ACTIVE
                OverlayService.OverlayState.ERROR -> OverlayState.ERROR
            }
            _overlayState.value = managerState
            notifyOverlayStateChanged(managerState)
        }
        
        override fun onOverlayAdded(overlayId: String) {
            notifyOverlayAdded(overlayId)
        }
        
        override fun onOverlayRemoved(overlayId: String) {
            notifyOverlayRemoved(overlayId)
        }
        
        override fun onOverlayError(error: String) {
            notifyOverlayError(error)
        }
    }
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    fun addListener(listener: OverlayManagerListener) {
        overlayListeners.add(listener)
    }
    
    fun removeListener(listener: OverlayManagerListener) {
        overlayListeners.remove(listener)
    }
    
    suspend fun startService(): Boolean {
        if (isServiceBound.get()) {
            return true
        }
        
        if (!permissionHandler.hasOverlayPermission()) {
            notifyOverlayError("Overlay permission not granted")
            return false
        }
        
        return try {
            _serviceState.value = ServiceState.CONNECTING
            val intent = Intent(context, OverlayService::class.java)
            context.startService(intent)
            context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            true
        } catch (e: Exception) {
            _serviceState.value = ServiceState.ERROR
            notifyOverlayError("Failed to start overlay service: ${e.message}")
            false
        }
    }
    
    fun stopService() {
        scope.launch {
            try {
                if (isServiceBound.get()) {
                    overlayService?.removeStateListener(overlayStateListener)
                    context.unbindService(serviceConnection)
                    isServiceBound.set(false)
                }
                
                val intent = Intent(context, OverlayService::class.java)
                context.stopService(intent)
                _serviceState.value = ServiceState.DISCONNECTED
                
            } catch (e: Exception) {
                notifyOverlayError("Error stopping overlay service: ${e.message}")
            }
        }
    }
    
    suspend fun addOverlay(
        overlayId: String, 
        overlayView: OverlayView, 
        layoutParams: WindowManager.LayoutParams? = null
    ): Boolean {
        if (!isServiceBound.get()) {
            if (!startService()) {
                return false
            }
            // Wait a bit for service to connect
            kotlinx.coroutines.delay(100)
        }
        
        return overlayService?.addOverlay(overlayId, overlayView, layoutParams) ?: false
    }
    
    fun removeOverlay(overlayId: String): Boolean {
        return overlayService?.removeOverlay(overlayId) ?: false
    }
    
    fun updateOverlayParams(overlayId: String, layoutParams: WindowManager.LayoutParams): Boolean {
        return overlayService?.updateOverlayParams(overlayId, layoutParams) ?: false
    }
    
    fun isOverlayVisible(overlayId: String): Boolean {
        return overlayService?.isOverlayVisible(overlayId) ?: false
    }
    
    fun getActiveOverlays(): Set<String> {
        return overlayService?.getActiveOverlays() ?: emptySet()
    }
    
    fun hideOverlay(overlayId: String): Boolean {
        return overlayService?.hideOverlay(overlayId) ?: false
    }
    
    fun showOverlay(overlayId: String): Boolean {
        return overlayService?.showOverlay(overlayId) ?: false
    }
    
    fun isServiceConnected(): Boolean = isServiceBound.get()
    
    fun getCurrentServiceState(): ServiceState = _serviceState.value
    
    fun getCurrentOverlayState(): OverlayState = _overlayState.value
    
    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        cleanup()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        cleanup()
    }
    
    private fun cleanup() {
        scope.cancel()
        stopService()
    }
    
    private fun notifyServiceStateChanged(state: ServiceState) {
        overlayListeners.forEach { it.onServiceStateChanged(state) }
    }
    
    private fun notifyOverlayStateChanged(state: OverlayState) {
        overlayListeners.forEach { it.onOverlayStateChanged(state) }
    }
    
    private fun notifyOverlayAdded(overlayId: String) {
        overlayListeners.forEach { it.onOverlayAdded(overlayId) }
    }
    
    private fun notifyOverlayRemoved(overlayId: String) {
        overlayListeners.forEach { it.onOverlayRemoved(overlayId) }
    }
    
    private fun notifyOverlayError(error: String) {
        overlayListeners.forEach { it.onOverlayError(error) }
    }
}