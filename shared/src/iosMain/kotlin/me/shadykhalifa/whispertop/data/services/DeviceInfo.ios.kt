package me.shadykhalifa.whispertop.data.services

import platform.UIKit.UIDevice
import platform.Foundation.NSBundle
import platform.Foundation.NSProcessInfo

actual fun getDeviceManufacturer(): String = "Apple"

actual fun getDeviceModel(): String = UIDevice.currentDevice.model

actual fun getOSVersion(): String = "${UIDevice.currentDevice.systemName} ${UIDevice.currentDevice.systemVersion}"

actual fun getAppVersion(): String = try {
    NSBundle.mainBundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "Unknown"
} catch (e: Exception) {
    "Unknown"
}

actual fun getCpuCoreCount(): Int = NSProcessInfo.processInfo.processorCount.toInt()

actual fun getArchitecture(): String = "ARM64" // iOS devices are typically ARM64