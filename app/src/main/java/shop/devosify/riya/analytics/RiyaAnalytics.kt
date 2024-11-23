@Singleton
class RiyaAnalytics @Inject constructor(
    private val firebaseAnalytics: FirebaseAnalytics,
    private val performanceTracker: PerformanceTracker,
    private val errorTracker: ErrorTracker
) {
    fun logEvent(
        eventName: String,
        params: Map<String, Any> = emptyMap()
    ) {
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
        firebaseAnalytics.logEvent(eventName, bundle)
    }

    fun logUserAction(action: UserAction) {
        logEvent("user_action", mapOf(
            "type" to action.type.name,
            "duration_ms" to action.duration,
            "success" to action.isSuccess,
            "context" to action.context
        ))
    }

    fun logError(
        type: String,
        message: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        logEvent("error", mapOf(
            "type" to type,
            "message" to message,
            "metadata" to metadata
        ))
        
        errorTracker.trackError(RiyaError(type, message), metadata)
    }

    fun startTrackingPerformance(operationName: String) {
        performanceTracker.startOperation(operationName)
    }

    fun endTrackingPerformance(
        operationName: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        performanceTracker.endOperation(operationName, metadata)
    }
}

data class UserAction(
    val type: ActionType,
    val duration: Long,
    val isSuccess: Boolean,
    val context: Map<String, Any> = emptyMap()
)

enum class ActionType {
    VOICE_COMMAND,
    MEMORY_ACCESS,
    AUTOMATION_TRIGGER,
    SYSTEM_INTEGRATION
} 