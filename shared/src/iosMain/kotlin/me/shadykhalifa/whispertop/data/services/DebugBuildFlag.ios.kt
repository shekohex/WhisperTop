package me.shadykhalifa.whispertop.data.services

import platform.Foundation.NSBundle

actual fun getDebugBuildFlag(): Boolean {
    return try {
        // Check for debug configuration in iOS bundle
        val bundle = NSBundle.mainBundle
        val debugFlag = bundle.objectForInfoDictionaryKey("DEBUG") as? Boolean
        debugFlag ?: false
    } catch (e: Exception) {
        // Fallback to false for release builds
        false
    }
}