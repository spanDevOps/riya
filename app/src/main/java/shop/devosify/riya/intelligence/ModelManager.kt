@Singleton
class ModelManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkUtils: NetworkUtils,
    private val analyticsService: AnalyticsService,
    private val preferences: RiyaPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _modelState = MutableStateFlow<Map<ModelType, ModelState>>(emptyMap())
    val modelState: StateFlow<Map<ModelType, ModelState>> = _modelState.asStateFlow()

    private val modelConfigs = mapOf(
        ModelType.EMBEDDING to ModelConfig(
            name = "mini_bert_embeddings",
            size = 25_000_000, // 25MB
            url = "https://storage.googleapis.com/riya-models/mini_bert_q.tflite",
            required = false
        ),
        ModelType.NLU to ModelConfig(
            name = "distilled_nlu",
            size = 15_000_000, // 15MB
            url = "https://storage.googleapis.com/riya-models/nlu_q.tflite",
            required = false
        )
    )

    init {
        scope.launch {
            checkInstalledModels()
            preferences.offlineIntelligenceMode.collect { mode ->
                when (mode) {
                    OfflineMode.ML_ONLY -> ensureModelsInstalled()
                    OfflineMode.HEURISTICS_ONLY -> cleanupUnusedModels()
                    OfflineMode.HYBRID -> {} // Keep current state
                }
            }
        }
    }

    suspend fun getModel(type: ModelType): TensorFlowLiteModel? {
        val state = _modelState.value[type] ?: return null
        if (!state.isReady) return null

        return when (type) {
            ModelType.EMBEDDING -> loadEmbeddingModel()
            ModelType.NLU -> loadNLUModel()
        }
    }

    suspend fun downloadModel(type: ModelType): Result<Unit> {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("No network connection"))
        }

        val config = modelConfigs[type] ?: return Result.failure(Exception("Unknown model type"))
        
        return try {
            updateModelState(type) { it.copy(isDownloading = true) }
            
            // Download model file
            val modelFile = downloadModelFile(config.url, config.name)
            
            // Verify model
            if (!verifyModel(modelFile)) {
                modelFile.delete()
                throw Exception("Model verification failed")
            }

            updateModelState(type) { 
                ModelState(
                    isInstalled = true,
                    isReady = true,
                    isDownloading = false
                )
            }
            
            analyticsService.logEvent("model_downloaded", mapOf(
                "type" to type.name,
                "size" to config.size
            ))
            
            Result.success(Unit)
        } catch (e: Exception) {
            updateModelState(type) { it.copy(isDownloading = false) }
            Result.failure(e)
        }
    }

    private suspend fun checkInstalledModels() {
        modelConfigs.forEach { (type, config) ->
            val modelFile = File(context.getDir("models", Context.MODE_PRIVATE), config.name)
            val state = if (modelFile.exists() && verifyModel(modelFile)) {
                ModelState(isInstalled = true, isReady = true)
            } else {
                ModelState()
            }
            _modelState.value = _modelState.value + (type to state)
        }
    }

    private suspend fun ensureModelsInstalled() {
        modelConfigs.forEach { (type, config) ->
            if (config.required && !isModelInstalled(type)) {
                downloadModel(type)
            }
        }
    }

    private fun cleanupUnusedModels() {
        val modelsDir = context.getDir("models", Context.MODE_PRIVATE)
        modelsDir.listFiles()?.forEach { it.delete() }
        _modelState.value = _modelState.value.mapValues { 
            it.value.copy(isInstalled = false, isReady = false)
        }
    }
}

enum class ModelType {
    EMBEDDING,
    NLU
}

data class ModelState(
    val isInstalled: Boolean = false,
    val isReady: Boolean = false,
    val isDownloading: Boolean = false,
    val error: String? = null
)

data class ModelConfig(
    val name: String,
    val size: Long,
    val url: String,
    val required: Boolean
)

enum class OfflineMode {
    ML_ONLY,      // Use ML models exclusively
    HEURISTICS_ONLY, // Use pattern matching and rules
    HYBRID        // Use ML when available, fallback to heuristics
} 