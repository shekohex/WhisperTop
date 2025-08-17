package me.shadykhalifa.whispertop.data.remote

import android.util.Log

actual fun apiLog(tag: String, message: String) {
    Log.d(tag, message)
}

actual fun apiLogError(tag: String, message: String, throwable: Throwable?) {
    if (throwable != null) {
        Log.e(tag, message, throwable)
    } else {
        Log.e(tag, message)
    }
}