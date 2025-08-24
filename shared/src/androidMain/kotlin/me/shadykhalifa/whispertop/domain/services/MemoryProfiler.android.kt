package me.shadykhalifa.whispertop.domain.services

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.domain.models.LeakSeverity
import me.shadykhalifa.whispertop.domain.models.LeakSuspicion
import me.shadykhalifa.whispertop.domain.models.MemoryInfo
import me.shadykhalifa.whispertop.domain.models.WeakReferenceInfo
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

actual class MemoryProfilerImpl(
    private val context: Context
) : MemoryProfiler {
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    private val weakReferences = ConcurrentHashMap<String, WeakReference<Any>>()
    private val referenceInfo = ConcurrentHashMap<String, WeakReferenceInfo>()
    private val objectCounts = ConcurrentHashMap<String, Int>()
    private val leakThresholds = mapOf(
        "ViewModel" to 5,
        "Fragment" to 3,
        "Activity" to 2,
        "Service" to 2,
        "BroadcastReceiver" to 3,
        "AudioRecorder" to 1,
        "TranscriptionSession" to 10
    )
    
    private val _memoryInfo = MutableStateFlow(getCurrentMemoryInfo())
    private val _leakSuspicions = MutableStateFlow<List<LeakSuspicion>>(emptyList())
    
    override fun getCurrentMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        // Get detailed memory information using Debug class
        val debugMemInfo = Debug.MemoryInfo()
        Debug.getMemoryInfo(debugMemInfo)
        
        val totalMemory = memInfo.totalMem
        val availableMemory = memInfo.availMem
        val usedMemory = totalMemory - availableMemory
        val maxMemory = Runtime.getRuntime().maxMemory()
        
        return MemoryInfo(
            totalMemory = totalMemory,
            freeMemory = availableMemory,
            usedMemory = usedMemory,
            maxMemory = maxMemory
        )
    }
    
    override fun trackObject(obj: Any, tag: String): String {
        val id = generateTrackingId(tag)
        val className = obj::class.simpleName ?: "Unknown"
        
        // Create weak reference
        val weakRef = WeakReference(obj)
        weakReferences[id] = weakRef
        
        // Store reference info
        referenceInfo[id] = WeakReferenceInfo(
            id = id,
            className = className,
            createdAt = Clock.System.now().toEpochMilliseconds()
        )
        
        // Update object counts
        objectCounts[className] = (objectCounts[className] ?: 0) + 1
        
        // Update memory info
        updateMemoryInfo()
        
        return id
    }
    
    override fun releaseObject(id: String) {
        val refInfo = referenceInfo[id]
        if (refInfo != null) {
            // Mark as cleared
            referenceInfo[id] = refInfo.copy(
                isCleared = true,
                clearedAt = Clock.System.now().toEpochMilliseconds()
            )
            
            // Update object counts
            val count = objectCounts[refInfo.className] ?: 0
            if (count > 0) {
                objectCounts[refInfo.className] = count - 1
            }
        }
        
        weakReferences.remove(id)
        updateMemoryInfo()
    }
    
    override fun performWeakReferenceCleanup(): Int {
        var cleanedCount = 0
        val iterator = weakReferences.iterator()
        
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val id = entry.key
            val weakRef = entry.value
            
            if (weakRef.get() == null) {
                // Object has been garbage collected
                iterator.remove()
                
                val refInfo = referenceInfo[id]
                if (refInfo != null && !refInfo.isCleared) {
                    referenceInfo[id] = refInfo.copy(
                        isCleared = true,
                        clearedAt = Clock.System.now().toEpochMilliseconds()
                    )
                    
                    // Update object counts
                    val count = objectCounts[refInfo.className] ?: 0
                    if (count > 0) {
                        objectCounts[refInfo.className] = count - 1
                    }
                }
                
                cleanedCount++
            }
        }
        
        if (cleanedCount > 0) {
            updateMemoryInfo()
        }
        
        return cleanedCount
    }
    
    override fun detectPotentialLeaks(): List<LeakSuspicion> {
        val suspicions = mutableListOf<LeakSuspicion>()
        val currentTime = Clock.System.now().toEpochMilliseconds()
        
        objectCounts.forEach { (className, count) ->
            val threshold = getThresholdForClass(className)
            if (count > threshold) {
                val severity = when {
                    count > threshold * 3 -> LeakSeverity.CRITICAL
                    count > threshold * 2 -> LeakSeverity.HIGH
                    count > threshold * 1.5 -> LeakSeverity.MEDIUM
                    else -> LeakSeverity.LOW
                }
                
                suspicions.add(
                    LeakSuspicion(
                        objectType = className,
                        referenceCount = count,
                        threshold = threshold,
                        detectedAt = currentTime,
                        severity = severity
                    )
                )
            }
        }
        
        _leakSuspicions.value = suspicions
        return suspicions
    }
    
    override fun observeMemoryInfo(): Flow<MemoryInfo> = _memoryInfo.asStateFlow()
    
    override fun observeLeakSuspicions(): Flow<List<LeakSuspicion>> = _leakSuspicions.asStateFlow()
    
    override fun getWeakReferenceInfo(): List<WeakReferenceInfo> {
        return referenceInfo.values.toList()
    }
    
    override fun suggestGarbageCollection() {
        // Perform cleanup first
        performWeakReferenceCleanup()
        
        // Suggest garbage collection
        System.gc()
        
        // Update memory info after cleanup
        updateMemoryInfo()
    }
    
    private fun updateMemoryInfo() {
        _memoryInfo.value = getCurrentMemoryInfo()
    }
    
    private fun generateTrackingId(tag: String): String {
        return "${tag}_${Clock.System.now().toEpochMilliseconds()}_${(0..999).random()}"
    }
    
    private fun getThresholdForClass(className: String): Int {
        return leakThresholds.entries.find { (key, _) ->
            className.contains(key, ignoreCase = true)
        }?.value ?: 5 // Default threshold
    }
}