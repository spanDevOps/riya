@Singleton
class ResourceAwareDecisionEngine @Inject constructor(
    private val systemMonitorService: SystemMonitorService,
    private val analyticsService: AnalyticsService,
    private val preferences: RiyaPreferences
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _currentMode = MutableStateFlow<ProcessingMode>(ProcessingMode.HYBRID)
    val currentMode: StateFlow<ProcessingMode> = _currentMode.asStateFlow()

    init {
        scope.launch {
            // Continuously monitor system resources and adjust processing mode
            combine(
                systemMonitorService.getBatteryFlow(),
                systemMonitorService.getMemoryFlow(),
                systemMonitorService.getCpuUsageFlow(),
                systemMonitorService.getTemperatureFlow(),
                preferences.performanceMode
            ) { battery, memory, cpu, temp, prefMode ->
                determineOptimalMode(
                    SystemResources(
                        batteryLevel = battery,
                        availableMemory = memory,
                        cpuUsage = cpu,
                        temperature = temp
                    ),
                    prefMode
                )
            }.collect { newMode ->
                if (_currentMode.value != newMode) {
                    _currentMode.value = newMode
                    analyticsService.logEvent("processing_mode_changed", mapOf(
                        "new_mode" to newMode.name,
                        "reason" to getCurrentSystemStatus()
                    ))
                }
            }
        }
    }

    private fun determineOptimalMode(
        resources: SystemResources,
        preferredMode: PerformanceMode
    ): ProcessingMode {
        // Quick return for user preferences
        when (preferredMode) {
            PerformanceMode.BATTERY_SAVER -> return ProcessingMode.HEURISTICS
            PerformanceMode.PERFORMANCE -> return ProcessingMode.ML_ONLY
            else -> {} // Continue with dynamic decision
        }

        // Score different aspects (-1 to 1, where 1 means resources are abundant)
        val batteryScore = (resources.batteryLevel - 50) / 50f // Below 50% starts getting negative
        val memoryScore = (resources.availableMemory - MEMORY_THRESHOLD) / MEMORY_THRESHOLD
        val cpuScore = 1 - (resources.cpuUsage / CPU_THRESHOLD)
        val tempScore = 1 - (resources.temperature / TEMP_THRESHOLD)

        // Weighted decision
        val resourceScore = (
            BATTERY_WEIGHT * batteryScore +
            MEMORY_WEIGHT * memoryScore +
            CPU_WEIGHT * cpuScore +
            TEMP_WEIGHT * tempScore
        )

        return when {
            resourceScore > 0.5 -> ProcessingMode.ML_ONLY      // Resources are abundant
            resourceScore > 0.0 -> ProcessingMode.HYBRID       // Resources are moderate
            else -> ProcessingMode.HEURISTICS                  // Resources are constrained
        }
    }

    suspend fun shouldUseMlModel(modelType: ModelType): Boolean {
        val currentResources = SystemResources(
            batteryLevel = systemMonitorService.getBatteryLevel(),
            availableMemory = systemMonitorService.getAvailableMemory(),
            cpuUsage = systemMonitorService.getCpuUsage(),
            temperature = systemMonitorService.getDeviceTemperature()
        )

        // Consider model-specific requirements
        val modelRequirements = getModelRequirements(modelType)
        
        return when (_currentMode.value) {
            ProcessingMode.ML_ONLY -> true
            ProcessingMode.HEURISTICS -> false
            ProcessingMode.HYBRID -> {
                // Check if resources meet model's minimum requirements
                currentResources.availableMemory >= modelRequirements.minMemory &&
                currentResources.batteryLevel >= modelRequirements.minBattery &&
                currentResources.cpuUsage <= modelRequirements.maxCpuUsage &&
                currentResources.temperature <= modelRequirements.maxTemperature
            }
        }
    }

    private fun getModelRequirements(modelType: ModelType): ModelRequirements {
        return when (modelType) {
            ModelType.EMBEDDING -> ModelRequirements(
                minMemory = 100_000_000,  // 100MB
                minBattery = 20,          // 20%
                maxCpuUsage = 70,         // 70%
                maxTemperature = 40f      // 40°C
            )
            ModelType.NLU -> ModelRequirements(
                minMemory = 150_000_000,  // 150MB
                minBattery = 30,          // 30%
                maxCpuUsage = 60,         // 60%
                maxTemperature = 38f      // 38°C
            )
        }
    }

    private fun getCurrentSystemStatus(): String {
        return buildString {
            append("Battery: ${systemMonitorService.getBatteryLevel()}%, ")
            append("Memory: ${systemMonitorService.getAvailableMemory() / 1_000_000}MB, ")
            append("CPU: ${systemMonitorService.getCpuUsage()}%, ")
            append("Temp: ${systemMonitorService.getDeviceTemperature()}°C")
        }
    }

    companion object {
        // Thresholds
        private const val MEMORY_THRESHOLD = 200_000_000L // 200MB
        private const val CPU_THRESHOLD = 80f // 80%
        private const val TEMP_THRESHOLD = 45f // 45°C

        // Weights for decision making
        private const val BATTERY_WEIGHT = 0.4f
        private const val MEMORY_WEIGHT = 0.3f
        private const val CPU_WEIGHT = 0.2f
        private const val TEMP_WEIGHT = 0.1f
    }
}

data class SystemResources(
    val batteryLevel: Int,          // Percentage
    val availableMemory: Long,      // Bytes
    val cpuUsage: Float,           // Percentage
    val temperature: Float         // Celsius
)

data class ModelRequirements(
    val minMemory: Long,           // Bytes
    val minBattery: Int,           // Percentage
    val maxCpuUsage: Float,        // Percentage
    val maxTemperature: Float      // Celsius
)

enum class ProcessingMode {
    ML_ONLY,         // Use ML models exclusively
    HEURISTICS,      // Use pattern matching and rules
    HYBRID           // Dynamically choose based on context
}

enum class PerformanceMode {
    BATTERY_SAVER,   // Prioritize battery life
    BALANCED,        // Dynamic based on conditions
    PERFORMANCE      // Prioritize intelligence/performance
} 