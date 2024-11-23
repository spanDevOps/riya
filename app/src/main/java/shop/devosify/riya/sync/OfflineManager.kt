@Singleton
class OfflineManager @Inject constructor(
    private val networkUtils: NetworkUtils,
    private val secureStorage: SecureStorage,
    private val memoryDao: MemoryDao,
    private val conversationDao: ConversationDao,
    private val workManager: WorkManager,
    private val analyticsService: AnalyticsService,
    private val ttsService: TtsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    init {
        observeNetworkStatus()
        scheduleSyncWork()
    }

    private fun observeNetworkStatus() {
        scope.launch {
            networkUtils.observeNetworkStatus().collect { isOnline ->
                if (isOnline) {
                    initiateSync()
                }
            }
        }
    }

    suspend fun cacheResponse(key: String, response: String, expirationTime: Long = 24.hours) {
        val cachedResponse = CachedResponse(
            data = response,
            timestamp = System.currentTimeMillis(),
            expiresAt = System.currentTimeMillis() + expirationTime
        )
        secureStorage.storeSensitiveData(
            "cache_$key",
            gson.toJson(cachedResponse).toByteArray()
        )
    }

    suspend fun getCachedResponse(key: String): String? {
        return secureStorage.retrieveSensitiveData("cache_$key")?.let { data ->
            val cached = gson.fromJson(
                data.toString(Charsets.UTF_8),
                CachedResponse::class.java
            )
            if (cached.isValid()) cached.data else null
        }
    }

    suspend fun queueOfflineAction(action: OfflineAction) {
        secureStorage.storeSensitiveData(
            "offline_action_${action.id}",
            gson.toJson(action).toByteArray()
        )
        analyticsService.logEvent("offline_action_queued", mapOf(
            "type" to action.type.name,
            "id" to action.id
        ))
    }

    private fun scheduleSyncWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWork = PeriodicWorkRequestBuilder<OfflineSyncWorker>(
            15, TimeUnit.MINUTES,
            5, TimeUnit.MINUTES
        ).setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.MINUTES)
            .build()

        workManager.enqueueUniquePeriodicWork(
            "offline_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWork
        )
    }

    private suspend fun initiateSync() {
        _syncStatus.value = SyncStatus.Syncing
        try {
            val actions = getAllPendingActions()
            
            if (actions.isNotEmpty()) {
                // Notify user that offline tasks are being processed
                ttsService.speak(
                    "I'm back online. Processing ${actions.size} pending tasks."
                )
            }
            
            actions.forEach { action ->
                processOfflineAction(action)
                removeProcessedAction(action.id)
            }
            
            if (actions.isNotEmpty()) {
                // Notify completion
                ttsService.speak(
                    "I've completed processing all pending tasks."
                )
            }
            
            _syncStatus.value = SyncStatus.Success(actions.size)
        } catch (e: Exception) {
            _syncStatus.value = SyncStatus.Error(e.message ?: "Sync failed")
            analyticsService.logError("sync_failure", e.message ?: "Unknown error")
            
            // Notify user about sync failure
            ttsService.speak(
                "I encountered an error while processing offline tasks. Please try again later."
            )
        }
    }

    private suspend fun processOfflineAction(action: OfflineAction) {
        when (action.type) {
            ActionType.VOICE_COMMAND -> processOfflineVoiceCommand(action)
            ActionType.MEMORY_CREATION -> processOfflineMemory(action)
            ActionType.AUTOMATION_RULE -> processOfflineAutomation(action)
        }
    }

    private suspend fun getAllPendingActions(): List<OfflineAction> {
        return secureStorage.getAllSensitiveData()
            .filter { it.key.startsWith("offline_action_") }
            .mapNotNull { (_, data) ->
                try {
                    gson.fromJson(
                        data.toString(Charsets.UTF_8),
                        OfflineAction::class.java
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }

    private suspend fun removeProcessedAction(id: String) {
        secureStorage.removeSensitiveData("offline_action_$id")
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object Syncing : SyncStatus()
        data class Success(val actionCount: Int) : SyncStatus()
        data class Error(val message: String) : SyncStatus()
    }
}

data class OfflineAction(
    val id: String = UUID.randomUUID().toString(),
    val type: ActionType,
    val data: Map<String, Any>,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ActionType {
    VOICE_COMMAND,
    MEMORY_CREATION,
    AUTOMATION_RULE
}

data class CachedResponse(
    val data: String,
    val timestamp: Long,
    val expiresAt: Long
) {
    fun isValid(): Boolean = System.currentTimeMillis() < expiresAt
} 