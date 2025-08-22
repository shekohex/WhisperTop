package me.shadykhalifa.whispertop.utils

import kotlinx.datetime.Clock

object TimeUtils {
    fun currentTimeMillis(): Long = Clock.System.now().toEpochMilliseconds()
}