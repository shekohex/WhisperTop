package me.shadykhalifa.whispertop.utils

actual object Logger {
    actual fun debug(tag: String, message: String) {
        android.util.Log.d(tag, message)
    }
    
    actual fun warn(tag: String, message: String) {
        android.util.Log.w(tag, message)
    }
    
    actual fun error(tag: String, message: String) {
        android.util.Log.e(tag, message)
    }
    
    actual fun wtf(tag: String, message: String) {
        android.util.Log.wtf(tag, message)
    }
}