package me.shadykhalifa.whispertop.data.services

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import me.shadykhalifa.whispertop.domain.services.ToastService

class ToastServiceImpl(private val context: Context) : ToastService {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun showToast(message: String, isLong: Boolean) {
        val duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT

        // Ensure toast is shown on main thread
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // Already on main thread
            Toast.makeText(context, message, duration).show()
        } else {
            // Post to main thread
            mainHandler.post {
                Toast.makeText(context, message, duration).show()
            }
        }
    }
}