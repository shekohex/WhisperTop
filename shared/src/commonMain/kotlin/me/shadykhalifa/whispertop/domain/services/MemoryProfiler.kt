package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.flow.Flow
import me.shadykhalifa.whispertop.domain.models.LeakSeverity
import me.shadykhalifa.whispertop.domain.models.LeakSuspicion
import me.shadykhalifa.whispertop.domain.models.MemoryInfo
import me.shadykhalifa.whispertop.domain.models.WeakReferenceInfo

interface MemoryProfiler {
    fun getCurrentMemoryInfo(): MemoryInfo
    fun trackObject(obj: Any, tag: String = obj::class.simpleName ?: "Unknown"): String
    fun releaseObject(id: String)
    fun performWeakReferenceCleanup(): Int
    fun detectPotentialLeaks(): List<LeakSuspicion>
    fun observeMemoryInfo(): Flow<MemoryInfo>
    fun observeLeakSuspicions(): Flow<List<LeakSuspicion>>
    fun getWeakReferenceInfo(): List<WeakReferenceInfo>
    fun suggestGarbageCollection()
}

expect class MemoryProfilerImpl() : MemoryProfiler


// Extension functions for easier usage
fun MemoryProfiler.trackViewModel(viewModel: Any): String {
    return trackObject(viewModel, "ViewModel")
}

fun MemoryProfiler.trackFragment(fragment: Any): String {
    return trackObject(fragment, "Fragment")
}

fun MemoryProfiler.trackService(service: Any): String {
    return trackObject(service, "Service")
}

fun MemoryProfiler.isMemoryUnderPressure(): Boolean {
    return getCurrentMemoryInfo().isMemoryPressure
}

fun MemoryProfiler.getMemoryUsagePercentage(): Double {
    return getCurrentMemoryInfo().usedMemoryPercentage
}