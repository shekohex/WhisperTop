package me.shadykhalifa.whispertop.data.repositories

import android.content.Context
import android.widget.Toast
import me.shadykhalifa.whispertop.domain.repositories.UserFeedbackRepository

class AndroidUserFeedbackRepository(
    private val context: Context
) : UserFeedbackRepository {
    
    override fun showFeedback(message: String, isError: Boolean) {
        val duration = if (isError) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
    }
    
    override fun showShortFeedback(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
    
    override fun showLongFeedback(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}