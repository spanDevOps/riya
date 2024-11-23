@Singleton
class ResourceMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: RiyaAnalytics
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _resourceMetrics = MutableStateFlow(ResourceMetrics())
    val resourceMetrics: StateFlow<ResourceMetrics> = _resourceMetrics.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            while (isActive) {
                val metrics = collectMetrics()
                _resourceMetrics.value = metrics
                
                // Log if thresholds exceeded
                checkThresholds(metrics)
                
                delay(MONITORING_INTERVAL)
            }
        }
    }

    private fun collectMetrics(): ResourceMetrics {
        return ResourceMetrics(
            memoryUsage = getMemoryUsage(),
            cpuUsage = getCpuUsage(),
            batteryLevel = getBatteryLevel(),
            diskSpace = getDiskSpace(),
            networkUsage = getNetworkUsage()
        )
    }

    private fun checkThresholds(metrics: ResourceMetrics) {
        if (metrics.memoryUsage > MEMORY_THRESHOLD) {
            analyticsService.logEvent("memory_warning", mapOf(
                "usage" to metrics.memoryUsage,
                "threshold" to MEMORY_THRESHOLD
            ))
        }
        
        if (metrics.cpuUsage > CPU_THRESHOLD) {
            analyticsService.logEvent("cpu_warning", mapOf(
                "usage" to metrics.cpuUsage,
                "threshold" to CPU_THRESHOLD
            ))
        }
        
        if (metrics.batteryLevel < BATTERY_THRESHOLD) {
            analyticsService.logEvent("battery_warning", mapOf(
                "level" to metrics.batteryLevel,
                "threshold" to BATTERY_THRESHOLD
            ))
        }
    }

    private fun getMemoryUsage(): Long {
        val runtime = Runtime.getRuntime()
        return (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024 // MB
    }

    private fun getCpuUsage(): Float {
        // Implement CPU usage calculation
        return 0f
    }

    private fun getBatteryLevel(): Int {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
    }

    companion object {
        private const val MONITORING_INTERVAL = 60_000L // 1 minute
        private const val MEMORY_THRESHOLD = 200L // MB
        private const val CPU_THRESHOLD = 80f // percent
        private const val BATTERY_THRESHOLD = 15 // percent
    }
}

data class ResourceMetrics(
    val memoryUsage: Long = 0L,
    val cpuUsage: Float = 0f,
    val batteryLevel: Int = 100,
    val diskSpace: Long = 0L,
    val networkUsage: NetworkMetrics = NetworkMetrics()
)

data class NetworkMetrics(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val latency: Long = 0L
) 