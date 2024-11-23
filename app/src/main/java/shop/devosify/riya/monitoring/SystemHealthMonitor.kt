@Singleton
class SystemHealthMonitor @Inject constructor(
    private val resourceMonitor: ResourceMonitor,
    private val networkUtils: NetworkUtils,
    private val analyticsService: RiyaAnalytics
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _healthStatus = MutableStateFlow(SystemHealth())
    val healthStatus: StateFlow<SystemHealth> = _healthStatus.asStateFlow()

    init {
        startMonitoring()
    }

    private fun startMonitoring() {
        scope.launch {
            combine(
                resourceMonitor.resourceMetrics,
                networkUtils.observeNetworkStatus(),
                observeServiceHealth()
            ) { resources, isOnline, services ->
                SystemHealth(
                    resources = resources,
                    isOnline = isOnline,
                    serviceStatus = services,
                    timestamp = System.currentTimeMillis()
                )
            }.collect { health ->
                _healthStatus.value = health
                checkHealthStatus(health)
            }
        }
    }

    private fun observeServiceHealth(): Flow<Map<String, ServiceStatus>> = flow {
        while (true) {
            val status = mapOf(
                "voice" to checkVoiceServices(),
                "memory" to checkMemorySystem(),
                "automation" to checkAutomationSystem(),
                "intelligence" to checkIntelligenceSystem()
            )
            emit(status)
            delay(SERVICE_CHECK_INTERVAL)
        }
    }

    private fun checkHealthStatus(health: SystemHealth) {
        // Check thresholds and log issues
        if (health.resources.memoryUsage > MEMORY_WARNING_THRESHOLD) {
            logHealthWarning("memory", "High memory usage: ${health.resources.memoryUsage}MB")
        }

        if (health.resources.cpuUsage > CPU_WARNING_THRESHOLD) {
            logHealthWarning("cpu", "High CPU usage: ${health.resources.cpuUsage}%")
        }

        health.serviceStatus.forEach { (service, status) ->
            if (!status.isHealthy) {
                logHealthWarning(service, "Service unhealthy: ${status.message}")
            }
        }
    }

    private fun logHealthWarning(component: String, message: String) {
        analyticsService.logEvent("health_warning", mapOf(
            "component" to component,
            "message" to message,
            "timestamp" to System.currentTimeMillis()
        ))
    }

    companion object {
        private const val SERVICE_CHECK_INTERVAL = 60_000L // 1 minute
        private const val MEMORY_WARNING_THRESHOLD = 200L // MB
        private const val CPU_WARNING_THRESHOLD = 80f // percent
    }
}

data class SystemHealth(
    val resources: ResourceMetrics = ResourceMetrics(),
    val isOnline: Boolean = true,
    val serviceStatus: Map<String, ServiceStatus> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

data class ServiceStatus(
    val isHealthy: Boolean,
    val message: String? = null,
    val lastChecked: Long = System.currentTimeMillis()
) 