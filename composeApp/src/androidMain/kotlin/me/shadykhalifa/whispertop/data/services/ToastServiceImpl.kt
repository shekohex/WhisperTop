package me.shadykhalifa.whispertop.data.services

import android.content.Context
import android.widget.Toast
import me.shadykhalifa.whispertop.domain.services.ToastService

class ToastServiceImpl(private val context: Context) : ToastService {
    
    override fun showToast(message: String, isLong: Boolean) {
        val duration = if (isLong) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(context, message, duration).show()
    }
}