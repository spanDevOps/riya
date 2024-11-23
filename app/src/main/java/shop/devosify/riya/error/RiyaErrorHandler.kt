@Singleton
class RiyaErrorHandler @Inject constructor(
    private val analyticsService: AnalyticsService,
    private val ttsService: TtsService
) {
    suspend fun handleError(error: RiyaError, context: String? = null) {
        // Log error
        analyticsService.logError(
            type = error.javaClass.simpleName,
            message = error.message ?: "Unknown error",
            context = context
        )

        // Provide user feedback
        val userMessage = when (error) {
            is RiyaError.NetworkError -> {
                if (error.isRetryable) {
                    "I'm having trouble connecting. Please try again."
                } else {
                    "Sorry, I'm offline right now."
                }
            }
            is RiyaError.APIError -> "I encountered a problem processing your request."
            is RiyaError.StorageError -> "I'm having trouble accessing my memory."
            is RiyaError.PermissionError -> "I need permission to ${error.permission} to help with that."
            is RiyaError.HardwareError -> "I'm having trouble with ${error.device}."
        }

        ttsService.speak(userMessage)
    }
} 