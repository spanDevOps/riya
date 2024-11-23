@Singleton
class KeyRotationService @Inject constructor(
    private val secureKeyManager: SecureKeyManager,
    private val apiKeyProvider: ApiKeyProvider,
    private val analyticsService: AnalyticsService,
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val workManager = WorkManager.getInstance(context)

    init {
        scheduleKeyRotation()
    }

    private fun scheduleKeyRotation() {
        val rotationWork = PeriodicWorkRequestBuilder<KeyRotationWorker>(
            30, TimeUnit.DAYS, // Rotate every 30 days
            1, TimeUnit.DAYS   // Flex period
        )
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true)
                .build()
        )
        .build()

        workManager.enqueueUniquePeriodicWork(
            "key_rotation",
            ExistingPeriodicWorkPolicy.KEEP,
            rotationWork
        )
    }

    suspend fun performKeyRotation() {
        try {
            // Rotate master encryption key
            secureKeyManager.rotateKey()
            
            // Rotate API key
            apiKeyProvider.rotateKey()
            
            analyticsService.logSecurityEvent("scheduled_key_rotation_success")
        } catch (e: Exception) {
            analyticsService.logError("scheduled_key_rotation", e.message ?: "Unknown error")
            throw SecurityException("Scheduled key rotation failed", e)
        }
    }

    fun cancelScheduledRotations() {
        workManager.cancelUniqueWork("key_rotation")
    }
}

@HiltWorker
class KeyRotationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val keyRotationService: KeyRotationService
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            keyRotationService.performKeyRotation()
            Result.success()
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
} 