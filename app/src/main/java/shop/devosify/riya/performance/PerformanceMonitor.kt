@Singleton
class PerformanceMonitor @Inject constructor(
    private val analyticsService: AnalyticsService,
    private val systemMonitorService: SystemMonitorService
) {
    private val metrics = ConcurrentHashMap<String, MetricData>()
    private val thresholds = PerformanceThresholds()

    suspend fun startOperation(operationName: String) {
        metrics[operationName] = MetricData(
            startTime = System.nanoTime(),
            memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(),
            batteryBefore = systemMonitorService.getCurrentState().batteryLevel
        )
    }

    suspend fun endOperation(operationName: String) {
        metrics[operationName]?.let { startData ->
            val duration = (System.nanoTime() - startData.startTime) / 1_000_000 // Convert to ms
            val memoryUsed = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) - startData.memoryBefore
            val batteryDrain = startData.batteryBefore - systemMonitorService.getCurrentState().batteryLevel

            // Check thresholds
            if (duration > thresholds.MAX_RESPONSE_TIME) {
                analyticsService.logPerformanceIssue(
                    "slow_operation",
                    "Operation $operationName took ${duration}ms"
                )
            }

            if (memoryUsed > thresholds.MAX_MEMORY_PER_OP) {
                analyticsService.logPerformanceIssue(
                    "high_memory",
                    "Operation $operationName used ${memoryUsed / 1024}KB"
                )
            }

            // Log metrics
            analyticsService.logPerformanceMetrics(
                operationName = operationName,
                duration = duration,
                memoryUsed = memoryUsed,
                batteryDrain = batteryDrain
            )
        }
    }

    private data class MetricData(
        val startTime: Long,
        val memoryBefore: Long,
        val batteryBefore: Int
    )
} 