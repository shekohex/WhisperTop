package me.shadykhalifa.whispertop.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class OpenAIModel(
    val modelId: String,
    val displayName: String,
    val description: String,
    val capabilities: ModelCapability,
    val pricing: PricingInfo,
    val useCase: ModelUseCase,
    val isCustom: Boolean = false
) {
    companion object {
        val PREDEFINED_MODELS = listOf(
            OpenAIModel(
                modelId = "whisper-1",
                displayName = "Whisper v1",
                description = "OpenAI's general-purpose speech recognition model with high accuracy",
                capabilities = ModelCapability.BALANCED,
                pricing = PricingInfo(pricePerMinute = 0.006),
                useCase = ModelUseCase.GENERAL_PURPOSE
            ),
            OpenAIModel(
                modelId = "whisper-3-turbo",
                displayName = "Whisper 3 Turbo",
                description = "Faster transcription with optimized performance for real-time applications",
                capabilities = ModelCapability.SPEED_OPTIMIZED,
                pricing = PricingInfo(pricePerMinute = 0.004),
                useCase = ModelUseCase.REAL_TIME
            ),
            OpenAIModel(
                modelId = "gpt-4o-audio-preview",
                displayName = "GPT-4o Audio Preview",
                description = "Advanced model with enhanced accuracy and understanding of audio context",
                capabilities = ModelCapability.ACCURACY_OPTIMIZED,
                pricing = PricingInfo(pricePerMinute = 0.012),
                useCase = ModelUseCase.HIGH_ACCURACY
            )
        )

        fun getModelById(modelId: String): OpenAIModel? {
            return PREDEFINED_MODELS.find { it.modelId == modelId }
        }

        fun getDefaultModel(): OpenAIModel = PREDEFINED_MODELS.first()
    }
}

@Serializable
enum class ModelCapability(
    val displayName: String,
    val description: String,
    val speedRating: Int, // 1-5 scale
    val accuracyRating: Int // 1-5 scale
) {
    SPEED_OPTIMIZED(
        "Speed Optimized",
        "Prioritizes fast transcription with good accuracy",
        speedRating = 5,
        accuracyRating = 3
    ),
    BALANCED(
        "Balanced",
        "Good balance of speed and accuracy for most use cases",
        speedRating = 3,
        accuracyRating = 4
    ),
    ACCURACY_OPTIMIZED(
        "Accuracy Optimized",
        "Highest accuracy with slightly longer processing time",
        speedRating = 2,
        accuracyRating = 5
    )
}

@Serializable
data class PricingInfo(
    val pricePerMinute: Double, // Price in USD per minute of audio
    val currency: String = "USD"
) {
    fun formatPrice(): String = "$${String.format("%.3f", pricePerMinute)}/$currency per minute"
}

@Serializable
enum class ModelUseCase(
    val displayName: String,
    val description: String,
    val recommendedFor: List<String>
) {
    GENERAL_PURPOSE(
        "General Purpose",
        "Best for everyday transcription needs",
        listOf("Voice notes", "Meetings", "Interviews", "General dictation")
    ),
    REAL_TIME(
        "Real-time",
        "Optimized for live transcription and quick responses",
        listOf("Live captions", "Voice commands", "Quick notes", "Chat transcription")
    ),
    HIGH_ACCURACY(
        "High Accuracy",
        "Best for professional or critical transcription work",
        listOf("Legal transcription", "Medical notes", "Academic research", "Professional documents")
    )
}