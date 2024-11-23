@Singleton
class GeofencingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val placesService: PlacesService,
    private val analyticsService: AnalyticsService,
    private val memoryDao: MemoryDao
) {
    private val geofencingClient = LocationServices.getGeofencingClient(context)
    private val _activeGeofences = MutableStateFlow<List<GeofenceData>>(emptyList())
    val activeGeofences: StateFlow<List<GeofenceData>> = _activeGeofences.asStateFlow()

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    suspend fun setupGeofencesForFrequentPlaces() {
        try {
            if (!hasLocationPermission()) {
                throw SecurityException("Required location permissions not granted")
            }

            // Get frequent places from PlacesService
            placesService.getFrequentLocations().collect { frequentLocations ->
                val significantLocations = frequentLocations
                    .filter { it.visitCount > MIN_VISITS_THRESHOLD }
                    .take(MAX_GEOFENCES)

                // Create geofences for each location
                val geofenceList = significantLocations.map { location ->
                    Geofence.Builder()
                        .setRequestId(location.placeId)
                        .setCircularRegion(
                            location.latitude,
                            location.longitude,
                            GEOFENCE_RADIUS_METERS
                        )
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setTransitionTypes(
                            Geofence.GEOFENCE_TRANSITION_ENTER or
                            Geofence.GEOFENCE_TRANSITION_EXIT or
                            Geofence.GEOFENCE_TRANSITION_DWELL
                        )
                        .setLoiteringDelay(DWELL_TIME_MS)
                        .build()
                }

                val geofencingRequest = GeofencingRequest.Builder()
                    .setInitialTrigger(
                        GeofencingRequest.INITIAL_TRIGGER_ENTER or
                        GeofencingRequest.INITIAL_TRIGGER_DWELL
                    )
                    .addGeofences(geofenceList)
                    .build()

                geofencingClient.removeGeofences(geofencePendingIntent)
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)

                // Update active geofences
                _activeGeofences.value = significantLocations.map { location ->
                    GeofenceData(
                        id = location.placeId,
                        name = location.placeName,
                        latitude = location.latitude,
                        longitude = location.longitude,
                        radius = GEOFENCE_RADIUS_METERS,
                        type = determineGeofenceType(location)
                    )
                }
            }
        } catch (e: Exception) {
            analyticsService.logError("geofence_setup", e.message ?: "Unknown error")
            throw e
        }
    }

    suspend fun handleGeofenceTransition(
        geofenceId: String,
        transitionType: Int
    ) {
        try {
            val geofence = _activeGeofences.value.find { it.id == geofenceId } ?: return
            val placeContext = placesService.getPlaceContext(geofence.toPlace())

            when (transitionType) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    handlePlaceEntry(geofence, placeContext)
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    handlePlaceExit(geofence, placeContext)
                }
                Geofence.GEOFENCE_TRANSITION_DWELL -> {
                    handlePlaceDwell(geofence, placeContext)
                }
            }
        } catch (e: Exception) {
            analyticsService.logError("geofence_handling", e.message ?: "Unknown error")
        }
    }

    private suspend fun handlePlaceEntry(
        geofence: GeofenceData,
        context: PlaceContext
    ) {
        // Create memory of place entry
        val memory = MemoryEntity(
            type = MemoryType.EXPERIENCE,
            content = "Arrived at ${geofence.name}",
            importance = calculateImportance(context),
            timestamp = System.currentTimeMillis(),
            tags = listOf("location", "arrival", geofence.type.toString().lowercase())
        )
        memoryDao.insertMemory(memory)

        // Trigger relevant automations
        when (geofence.type) {
            GeofenceType.HOME -> handleHomeArrival(context)
            GeofenceType.WORK -> handleWorkArrival(context)
            GeofenceType.FREQUENT -> handleFrequentPlaceArrival(context)
        }
    }

    private suspend fun handlePlaceExit(
        geofence: GeofenceData,
        context: PlaceContext
    ) {
        val memory = MemoryEntity(
            type = MemoryType.EXPERIENCE,
            content = "Left ${geofence.name}",
            importance = calculateImportance(context),
            timestamp = System.currentTimeMillis(),
            tags = listOf("location", "departure", geofence.type.toString().lowercase())
        )
        memoryDao.insertMemory(memory)
    }

    private suspend fun handlePlaceDwell(
        geofence: GeofenceData,
        context: PlaceContext
    ) {
        // Handle extended stay at location
        if (context.commonActivities.isNotEmpty()) {
            val memory = MemoryEntity(
                type = MemoryType.HABIT,
                content = "Spent time at ${geofence.name}, typically: " +
                        context.commonActivities.joinToString(", "),
                importance = 3,
                timestamp = System.currentTimeMillis(),
                tags = listOf("location", "dwell", "activity")
            )
            memoryDao.insertMemory(memory)
        }
    }

    private fun calculateImportance(context: PlaceContext): Int {
        return when {
            context.visitFrequency > 0.8f -> 5 // Very frequent
            context.visitFrequency > 0.5f -> 4 // Regular
            context.visitFrequency > 0.3f -> 3 // Occasional
            context.visitFrequency > 0.1f -> 2 // Rare
            else -> 1 // First time or very rare
        }
    }

    private fun hasLocationPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == 
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val GEOFENCE_RADIUS_METERS = 100f
        private const val DWELL_TIME_MS = 5 * 60 * 1000 // 5 minutes
        private const val MIN_VISITS_THRESHOLD = 3
        private const val MAX_GEOFENCES = 100
    }
}

data class GeofenceData(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val type: GeofenceType
)

enum class GeofenceType {
    HOME,
    WORK,
    FREQUENT
} 