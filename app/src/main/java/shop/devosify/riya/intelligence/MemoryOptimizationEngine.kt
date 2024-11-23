@Singleton
class MemoryOptimizationEngine @Inject constructor(
    private val memoryDao: MemoryDao,
    private val memoryCompressionService: MemoryCompressionService,
    private val systemMonitorService: SystemMonitorService,
    private val analyticsService: AnalyticsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _optimizationStatus = MutableStateFlow<OptimizationStatus>(OptimizationStatus.Idle)
    val optimizationStatus: StateFlow<OptimizationStatus> = _optimizationStatus.asStateFlow()

    init {
        scope.launch {
            // Monitor system resources and trigger optimization when needed
            systemMonitorService.getSystemState().collect { state ->
                if (shouldOptimize(state)) {
                    optimizeMemories()
                }
            }
        }
    }

    private suspend fun shouldOptimize(systemState: SystemState): Boolean {
        return systemState.availableStorage < LOW_STORAGE_THRESHOLD ||
               systemState.memoryUsage > HIGH_MEMORY_THRESHOLD ||
               systemState.batteryLevel < LOW_BATTERY_THRESHOLD
    }

    suspend fun optimizeMemories() {
        try {
            _optimizationStatus.value = OptimizationStatus.Optimizing
            
            // Get all memories sorted by importance
            val memories = memoryDao.getAllMemories()
                .sortedBy { it.importance }

            // Calculate target size based on available storage
            val targetSize = calculateTargetSize()
            var currentSize = memories.sumOf { it.content.length }

            // Optimize until we reach target size
            for (memory in memories) {
                if (currentSize <= targetSize) break

                when {
                    // Remove low importance memories if very old
                    memory.importance <= 2 && 
                    memory.timestamp < System.currentTimeMillis() - OLD_MEMORY_THRESHOLD -> {
                        memoryDao.deleteMemory(memory.id)
                        currentSize -= memory.content.length
                    }
                    
                    // Compress medium importance memories
                    memory.importance in 3..4 -> {
                        val compressed = memoryCompressionService.compressMemory(memory)
                        memoryDao.updateMemory(memory.copy(
                            content = compressed.data.toString(Charsets.UTF_8)
                        ))
                        currentSize -= (memory.content.length - compressed.data.size)
                    }
                    
                    // Keep high importance memories as is
                    else -> continue
                }
            }

            _optimizationStatus.value = OptimizationStatus.Success(
                memoriesOptimized = memories.size,
                spaceFreed = currentSize - targetSize
            )
            
            analyticsService.logEvent("memory_optimization_complete", mapOf(
                "memories_processed" to memories.size,
                "space_freed" to (currentSize - targetSize)
            ))

        } catch (e: Exception) {
            analyticsService.logError("memory_optimization", e.message ?: "Unknown error")
            _optimizationStatus.value = OptimizationStatus.Error(e.message ?: "Optimization failed")
        }
    }

    private suspend fun calculateTargetSize(): Int {
        val systemState = systemMonitorService.getCurrentState()
        val availableStorage = systemState.availableStorage
        
        // Target 70% of available storage for memories
        return (availableStorage * 0.7).toInt()
    }

    companion object {
        private const val LOW_STORAGE_THRESHOLD = 100 * 1024 * 1024L  // 100MB
        private const val HIGH_MEMORY_THRESHOLD = 80  // 80% usage
        private const val LOW_BATTERY_THRESHOLD = 20  // 20% battery
        private const val OLD_MEMORY_THRESHOLD = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}

sealed class OptimizationStatus {
    object Idle : OptimizationStatus()
    object Optimizing : OptimizationStatus()
    data class Success(
        val memoriesOptimized: Int,
        val spaceFreed: Int
    ) : OptimizationStatus()
    data class Error(val message: String) : OptimizationStatus()
}

data class OptimizationMetrics(
    val originalSize: Int,
    val optimizedSize: Int,
    val memoriesProcessed: Int,
    val compressionRatio: Float,
    val duration: Long
)

enum class OptimizationStrategy {
    AGGRESSIVE,    // Delete more, compress more
    BALANCED,      // Default strategy
    CONSERVATIVE   // Minimal optimization
} 