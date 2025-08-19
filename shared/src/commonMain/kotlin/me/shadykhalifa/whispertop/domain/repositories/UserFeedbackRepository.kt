package me.shadykhalifa.whispertop.domain.repositories

interface UserFeedbackRepository {
    fun showFeedback(message: String, isError: Boolean = false)
    fun showShortFeedback(message: String)
    fun showLongFeedback(message: String)
}