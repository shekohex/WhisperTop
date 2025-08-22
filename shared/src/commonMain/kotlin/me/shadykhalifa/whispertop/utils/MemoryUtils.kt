package me.shadykhalifa.whispertop.utils

expect object MemoryUtils {
    fun getUsedMemoryBytes(): Long
    fun getTotalMemoryBytes(): Long
    fun getMaxMemoryBytes(): Long
}