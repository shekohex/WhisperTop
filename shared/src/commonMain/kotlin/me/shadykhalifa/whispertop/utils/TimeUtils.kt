package me.shadykhalifa.whispertop.utils

import kotlinx.datetime.Clock

object TimeUtils {
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
    
    fun startOfDay(timeMillis: Long): Long {
        return timeMillis - (timeMillis % (24 * 60 * 60 * 1000))
    }
    
    fun endOfDay(timeMillis: Long): Long {
        return startOfDay(timeMillis) + (24 * 60 * 60 * 1000) - 1
    }
}