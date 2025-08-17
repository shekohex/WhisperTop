package me.shadykhalifa.whispertop.data.services

import android.os.Build

actual fun getDeviceManufacturer(): String = Build.MANUFACTURER

actual fun getDeviceModel(): String = Build.MODEL

actual fun getOSVersion(): String = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

actual fun getAppVersion(): String = "1.0.0" // Will be updated with actual BuildConfig later

actual fun getCpuCoreCount(): Int = Runtime.getRuntime().availableProcessors()

actual fun getArchitecture(): String = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"