package me.shadykhalifa.whispertop.domain.services

interface ToastService {
    fun showToast(message: String, isLong: Boolean = false)
}