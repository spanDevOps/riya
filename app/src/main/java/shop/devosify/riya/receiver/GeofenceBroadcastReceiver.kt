class GeofenceBroadcastReceiver : BroadcastReceiver() {
    @Inject lateinit var geofencingService: GeofencingService
    @Inject lateinit var analyticsService: AnalyticsService
    
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == GeofencingEvent.GEOFENCE_TRANSITION_BROADCAST) {
            val geofencingEvent = GeofencingEvent.fromIntent(intent)
            
            if (geofencingEvent.hasError()) {
                analyticsService.logError(
                    "geofence_transition",
                    "Error code: ${geofencingEvent.errorCode}"
                )
                return
            }

            val geofenceTransition = geofencingEvent.geofenceTransition
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        geofencingService.handleGeofenceTransition(
                            geofenceId = geofence.requestId,
                            transitionType = geofenceTransition
                        )
                    } catch (e: Exception) {
                        analyticsService.logError(
                            "geofence_handling",
                            e.message ?: "Unknown error"
                        )
                    }
                }
            }
        }
    }
} 