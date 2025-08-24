package me.shadykhalifa.whispertop.presentation.viewmodels

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
import me.shadykhalifa.whispertop.utils.Result

data class OnboardingWpmUiState(
    val currentStep: WpmOnboardingStep = WpmOnboardingStep.WELCOME,
    val selectedAge: Int? = null,
    val suggestedWpm: Int = 36,
    val manualWpmInput: String = "",
    val finalWpm: Int = 36,
    val isManualInputMode: Boolean = false,
    val showTypingTest: Boolean = false,
    val typingTestResult: Int? = null,
    val isTypingTestInProgress: Boolean = false,
    val typingTestProgress: Float = 0f,
    val typingTestTimeRemaining: Int = 60,
    val validationErrors: Map<String, String> = emptyMap(),
    val isCompleted: Boolean = false,
    val isSaving: Boolean = false
)

enum class WpmOnboardingStep(val stepNumber: Int) {
    WELCOME(0),
    AGE_SELECTION(1),
    WPM_INPUT(2),
    TYPING_TEST(3),
    CONFIRMATION(4),
    COMPLETE(5);
    
    companion object {
        fun getStepByNumber(number: Int): WpmOnboardingStep? = values().find { it.stepNumber == number }
    }
}

data class AgeGroup(
    val range: String,
    val minAge: Int,
    val maxAge: Int,
    val suggestedWpm: Int,
    val description: String
) {
    companion object {
        val AGE_GROUPS = listOf(
            AgeGroup("10-19", 10, 19, 40, "Young learners with good digital exposure"),
            AgeGroup("20-29", 20, 29, 60, "Peak performance age group"),
            AgeGroup("30-39", 30, 39, 50, "Experienced professionals"),
            AgeGroup("40-49", 40, 49, 45, "Mature professionals"),
            AgeGroup("50+", 50, 100, 40, "Adjusting to motor skill changes")
        )
        
        fun getAgeGroup(age: Int): AgeGroup? = AGE_GROUPS.find { age in it.minAge..it.maxAge }
        fun getSuggestedWpm(age: Int): Int = getAgeGroup(age)?.suggestedWpm ?: 36
    }
}

class OnboardingWpmViewModel(
    private val settingsRepository: SettingsRepository,
    errorHandler: ViewModelErrorHandler
) : BaseViewModel(errorHandler) {

    private val _uiState = MutableStateFlow(OnboardingWpmUiState())
    val uiState: StateFlow<OnboardingWpmUiState> = _uiState.asStateFlow()
    
    companion object {
        const val MIN_WPM = 20
        const val MAX_WPM = 60
        const val MIN_AGE = 10
        const val MAX_AGE = 100
        const val TYPING_TEST_DURATION_SECONDS = 60
        
        // Sample text for typing test
        const val TYPING_TEST_TEXT = "The quick brown fox jumps over the lazy dog. This sample text is designed to test your typing speed and accuracy. Try to type as naturally and quickly as possible without making mistakes."
    }

    fun navigateToStep(step: WpmOnboardingStep) {
        _uiState.value = _uiState.value.copy(currentStep = step)
    }

    fun proceedToNextStep() {
        val currentState = _uiState.value
        val nextStepNumber = currentState.currentStep.stepNumber + 1
        val nextStep = WpmOnboardingStep.getStepByNumber(nextStepNumber)
        
        if (nextStep != null) {
            // Skip typing test if user doesn't want it
            if (nextStep == WpmOnboardingStep.TYPING_TEST && !currentState.showTypingTest) {
                val confirmationStep = WpmOnboardingStep.getStepByNumber(nextStepNumber + 1)
                confirmationStep?.let { navigateToStep(it) }
            } else {
                navigateToStep(nextStep)
            }
        }
    }

    fun goToPreviousStep() {
        val currentState = _uiState.value
        val previousStepNumber = currentState.currentStep.stepNumber - 1
        val previousStep = WpmOnboardingStep.getStepByNumber(previousStepNumber)
        
        if (previousStep != null) {
            navigateToStep(previousStep)
        }
    }

    fun setAge(age: Int) {
        if (age in MIN_AGE..MAX_AGE) {
            val suggestedWpm = AgeGroup.getSuggestedWpm(age)
            _uiState.value = _uiState.value.copy(
                selectedAge = age,
                suggestedWpm = suggestedWpm,
                finalWpm = suggestedWpm,
                validationErrors = _uiState.value.validationErrors - "age"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                validationErrors = _uiState.value.validationErrors + ("age" to "Age must be between $MIN_AGE and $MAX_AGE")
            )
        }
    }

    fun setManualWpmInput(input: String) {
        _uiState.value = _uiState.value.copy(
            manualWpmInput = input,
            validationErrors = _uiState.value.validationErrors - "wpm"
        )
        
        // Try to parse and validate the input
        val wpm = input.toIntOrNull()
        if (wpm != null) {
            validateAndSetWpm(wpm)
        }
    }

    fun toggleManualInputMode() {
        val currentState = _uiState.value
        _uiState.value = currentState.copy(
            isManualInputMode = !currentState.isManualInputMode,
            manualWpmInput = if (!currentState.isManualInputMode) currentState.finalWpm.toString() else "",
            validationErrors = emptyMap()
        )
    }

    private fun validateAndSetWpm(wpm: Int) {
        if (wpm in MIN_WPM..MAX_WPM) {
            _uiState.value = _uiState.value.copy(
                finalWpm = wpm,
                validationErrors = _uiState.value.validationErrors - "wpm"
            )
        } else {
            _uiState.value = _uiState.value.copy(
                validationErrors = _uiState.value.validationErrors + ("wpm" to "WPM must be between $MIN_WPM and $MAX_WPM")
            )
        }
    }

    fun setShowTypingTest(show: Boolean) {
        _uiState.value = _uiState.value.copy(showTypingTest = show)
    }

    fun startTypingTest() {
        _uiState.value = _uiState.value.copy(
            isTypingTestInProgress = true,
            typingTestProgress = 0f,
            typingTestTimeRemaining = TYPING_TEST_DURATION_SECONDS,
            typingTestResult = null
        )
        
        // Start typing test timer
        launchSafely {
            // This would need platform-specific implementation for the actual typing test
            // For now, simulate a typing test completion
            kotlinx.coroutines.delay(1000) // Simulate test duration
            completeTypingTest(45) // Simulate result
        }
    }

    fun updateTypingTestProgress(wordsTyped: Int, secondsElapsed: Int) {
        val progress = (secondsElapsed.toFloat() / TYPING_TEST_DURATION_SECONDS)
        val timeRemaining = TYPING_TEST_DURATION_SECONDS - secondsElapsed
        
        _uiState.value = _uiState.value.copy(
            typingTestProgress = progress,
            typingTestTimeRemaining = timeRemaining
        )
        
        if (secondsElapsed >= TYPING_TEST_DURATION_SECONDS) {
            val wpm = (wordsTyped * 60) / TYPING_TEST_DURATION_SECONDS
            completeTypingTest(wpm)
        }
    }

    private fun completeTypingTest(wpm: Int) {
        val validatedWpm = wpm.coerceIn(MIN_WPM, MAX_WPM)
        _uiState.value = _uiState.value.copy(
            isTypingTestInProgress = false,
            typingTestResult = validatedWpm,
            finalWpm = validatedWpm,
            typingTestProgress = 1f,
            typingTestTimeRemaining = 0
        )
    }

    fun skipTypingTest() {
        _uiState.value = _uiState.value.copy(
            isTypingTestInProgress = false,
            showTypingTest = false
        )
        proceedToNextStep()
    }

    fun saveWpmConfiguration() {
        val currentState = _uiState.value
        
        // Validate final WPM
        if (currentState.finalWpm !in MIN_WPM..MAX_WPM) {
            _uiState.value = currentState.copy(
                validationErrors = currentState.validationErrors + ("final_wpm" to "Invalid WPM value")
            )
            return
        }

        launchSafely(
            onError = { throwable ->
                _uiState.value = _uiState.value.copy(isSaving = false)
                handleError(throwable, "Failed to save WPM configuration")
            }
        ) {
            _uiState.value = currentState.copy(isSaving = true)
            
            // Save WPM and mark onboarding as completed
            val wpmResult = settingsRepository.updateWordsPerMinute(currentState.finalWpm)
            val onboardingResult = settingsRepository.updateWpmOnboardingCompleted(true)
            
            when {
                wpmResult is Result.Error -> {
                    handleError(Exception("Failed to save WPM: ${wpmResult.exception.message}"))
                }
                onboardingResult is Result.Error -> {
                    handleError(Exception("Failed to save onboarding status: ${onboardingResult.exception.message}"))
                }
                else -> {
                    _uiState.value = currentState.copy(
                        isSaving = false,
                        isCompleted = true,
                        currentStep = WpmOnboardingStep.COMPLETE
                    )
                }
            }
        }
    }

    fun restartOnboarding() {
        _uiState.value = OnboardingWpmUiState()
    }

    fun getWpmRecommendationText(age: Int?): String {
        return if (age != null) {
            val ageGroup = AgeGroup.getAgeGroup(age)
            ageGroup?.let { group ->
                "Based on your age (${group.range} years), we recommend ${group.suggestedWpm} WPM. ${group.description}."
            } ?: "We recommend 36 WPM as a good starting point for mobile typing."
        } else {
            "We recommend 36 WPM as a good starting point for mobile typing."
        }
    }
    
    fun canProceedFromCurrentStep(): Boolean {
        val currentState = _uiState.value
        return when (currentState.currentStep) {
            WpmOnboardingStep.WELCOME -> true
            WpmOnboardingStep.AGE_SELECTION -> currentState.selectedAge != null && currentState.validationErrors["age"] == null
            WpmOnboardingStep.WPM_INPUT -> {
                if (currentState.isManualInputMode) {
                    currentState.manualWpmInput.isNotBlank() && 
                    currentState.validationErrors["wpm"] == null &&
                    currentState.finalWpm in MIN_WPM..MAX_WPM
                } else {
                    currentState.finalWpm in MIN_WPM..MAX_WPM
                }
            }
            WpmOnboardingStep.TYPING_TEST -> !currentState.isTypingTestInProgress
            WpmOnboardingStep.CONFIRMATION -> currentState.finalWpm in MIN_WPM..MAX_WPM
            WpmOnboardingStep.COMPLETE -> true
        }
    }
}