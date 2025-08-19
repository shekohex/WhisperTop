package me.shadykhalifa.whispertop.domain.usecases

import me.shadykhalifa.whispertop.domain.repositories.UserFeedbackRepository

class UserFeedbackUseCase(
    private val userFeedbackRepository: UserFeedbackRepository
) {
    fun showFeedback(message: String, isError: Boolean = false) {
        userFeedbackRepository.showFeedback(message, isError)
    }
    
    fun showShortFeedback(message: String) {
        userFeedbackRepository.showShortFeedback(message)
    }
    
    fun showLongFeedback(message: String) {
        userFeedbackRepository.showLongFeedback(message)
    }
}