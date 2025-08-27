package me.shadykhalifa.whispertop.data.repositories

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import me.shadykhalifa.whispertop.domain.repositories.UserFeedbackRepository

class AndroidUserFeedbackRepository(
    private val context: Context
) : UserFeedbackRepository {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun showFeedback(message: String, isError: Boolean) {
        val duration = if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        showToastOnMainThread(message, duration)
    }

    override fun showShortFeedback(message: String) {
        showToastOnMainThread(message, Toast.LENGTH_SHORT)
    }

    override fun showLongFeedback(message: String) {
        showToastOnMainThread(message, Toast.LENGTH_LONG)
    }

    private fun showToastOnMainThread(message: String, duration: Int) {
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