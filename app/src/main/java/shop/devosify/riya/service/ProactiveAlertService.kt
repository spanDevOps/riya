@Singleton
class ProactiveAlertService @Inject constructor(
    private val informationService: InformationService,
    private val userActivityMonitor: UserActivityMonitor,
    private val locationService: LocationService,
    private val ttsService: TtsService,
    private val notificationManager: NotificationManager
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            monitorImportantUpdates()
        }
    }

    private suspend fun monitorImportantUpdates() {
        coroutineScope {
            // Monitor route conditions
            launch { monitorRouteConditions() }
            
            // Monitor weather alerts
            launch { monitorWeatherAlerts() }
            
            // Monitor news related to user interests
            launch { monitorRelevantNews() }
            
            // Monitor calendar conflicts
            launch { monitorScheduleConflicts() }
        }
    }

    private suspend fun monitorRouteConditions() {
        while (true) {
            try {
                val workRoute = informationService.checkRouteStatus("work")
                if (workRoute.hasIssues) {
                    notifyUser(
                        title = "Route Alert",
                        message = "Issue detected on your work route: ${workRoute.description}",
                        priority = NotificationPriority.HIGH
                    )
                }
                delay(5.minutes)
            } catch (e: Exception) {
                delay(1.minutes)
            }
        }
    }

    private suspend fun notifyUser(
        title: String,
        message: String,
        priority: NotificationPriority
    ) {
        // Voice notification if appropriate
        if (priority == NotificationPriority.HIGH) {
            ttsService.speak(message)
        }

        // Visual notification
        notificationManager.showNotification(
            title = title,
            content = message,
            priority = priority
        )
    }
} 