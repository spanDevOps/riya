@Singleton
class PerformanceTracker @Inject constructor(
    private val analyticsService: AnalyticsService
) {
    private val metrics = mutableMapOf<String, MetricData>()
    
    fun startOperation(name: String) {
        metrics[name] = MetricData(
            startTime = System.nanoTime(),
            metadata = mutableMapOf()
        )
    }

    fun endOperation(name: String, metadata: Map<String, Any> = emptyMap()) {
        metrics[name]?.let { data ->
            val duration = (System.nanoTime() - data.startTime) / 1_000_000 // Convert to ms
            
            analyticsService.logEvent("performance_metric", mapOf(
                "operation" to name,
                "duration_ms" to duration,
                "metadata" to metadata
            ))
            
            metrics.remove(name)
        }
    }

    fun trackMemoryUsage() {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
        
        analyticsService.logEvent("memory_usage", mapOf(
            "used_mb" to usedMemory,
            "total_mb" to runtime.totalMemory() / 1024 / 1024
        ))
    }
}

private data class MetricData(
    val startTime: Long,
    val metadata: MutableMap<String, Any>
) 