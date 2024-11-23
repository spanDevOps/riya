@Singleton
class ErrorTracker @Inject constructor(
    private val analyticsService: AnalyticsService,
    private val networkUtils: NetworkUtils
) {
    private val _errors = MutableStateFlow<List<ErrorEvent>>(emptyList())
    val errors: StateFlow<List<ErrorEvent>> = _errors.asStateFlow()

    suspend fun trackError(error: RiyaError, context: Map<String, Any> = emptyMap()) {
        val errorEvent = ErrorEvent(
            error = error,
            context = context,
            timestamp = System.currentTimeMillis(),
            networkStatus = networkUtils.isNetworkAvailable(),
            deviceInfo = collectDeviceInfo()
        )
        
        _errors.update { current -> current + errorEvent }
        analyticsService.logError(
            type = error.javaClass.simpleName,
            message = error.message ?: "Unknown error",
            metadata = context
        )
    }

    private fun collectDeviceInfo(): Map<String, Any> {
        return mapOf(
            "device_model" to Build.MODEL,
            "android_version" to Build.VERSION.SDK_INT,
            "app_version" to BuildConfig.VERSION_NAME
        )
    }
} 