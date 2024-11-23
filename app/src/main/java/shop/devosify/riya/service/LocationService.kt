@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    private val _currentLocation = MutableStateFlow<Location?>(null)
    private val _locationUpdates = MutableStateFlow<List<LocationUpdate>>(emptyList())
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                _currentLocation.value = location
                recordLocationUpdate(location)
            }
        }
    }

    init {
        startLocationUpdates()
    }

    fun getCurrentLocation(): Flow<Location> = flow {
        if (!hasLocationPermission()) {
            throw SecurityException("Location permission not granted")
        }

        // Try to emit last known location first
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                _currentLocation.value = it
            }
        }

        // Then emit location updates
        _currentLocation.filterNotNull().collect { location ->
            emit(location)
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) return

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setIntervalMillis(UPDATE_INTERVAL)
            .setMinUpdateIntervalMillis(FASTEST_INTERVAL)
            .build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            analyticsService.logError("location_updates", e.message ?: "Permission denied")
        }
    }

    suspend fun getLocationHistory(timeRange: TimeRange): Flow<List<LocationUpdate>> = flow {
        val history = _locationUpdates.value.filter { update ->
            update.timestamp >= timeRange.start && update.timestamp <= timeRange.end
        }
        emit(history)
    }

    suspend fun getFrequentLocations(): Flow<List<FrequentLocation>> = flow {
        val updates = _locationUpdates.value
        val frequentLocations = updates
            .groupBy { update ->
                // Group by approximate location (rounded coordinates)
                Pair(
                    (update.latitude * 1000).toInt() / 1000.0,
                    (update.longitude * 1000).toInt() / 1000.0
                )
            }
            .map { (coordinates, visits) ->
                FrequentLocation(
                    latitude = coordinates.first,
                    longitude = coordinates.second,
                    visitCount = visits.size,
                    averageDuration = calculateAverageDuration(visits),
                    commonTimeRanges = findCommonTimeRanges(visits),
                    lastVisit = visits.maxOf { it.timestamp }
                )
            }
            .sortedByDescending { it.visitCount }

        emit(frequentLocations)
    }

    suspend fun getLocationContext(location: Location): LocationContext {
        val nearbyPlaces = getNearbyPlaces(location)
        val frequentVisits = getVisitFrequency(location)
        val timePatterns = getTimePatterns(location)

        return LocationContext(
            location = getLocationName(location),
            frequency = frequentVisits.frequency,
            associatedActivities = frequentVisits.activities
        )
    }

    private fun hasLocationPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == 
            PackageManager.PERMISSION_GRANTED
    }

    private fun recordLocationUpdate(location: Location) {
        val updates = _locationUpdates.value.toMutableList()
        updates.add(
            LocationUpdate(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis(),
                accuracy = location.accuracy
            )
        )
        // Keep only recent history
        if (updates.size > MAX_HISTORY_SIZE) {
            updates.removeAt(0)
        }
        _locationUpdates.value = updates
    }

    private fun calculateAverageDuration(visits: List<LocationUpdate>): Long {
        // Implementation for calculating average visit duration
        return 0L // Placeholder
    }

    private fun findCommonTimeRanges(visits: List<LocationUpdate>): List<TimeRange> {
        // Implementation for finding common time ranges of visits
        return emptyList() // Placeholder
    }

    companion object {
        private const val UPDATE_INTERVAL = 5 * 60 * 1000L // 5 minutes
        private const val FASTEST_INTERVAL = 3 * 60 * 1000L // 3 minutes
        private const val MAX_HISTORY_SIZE = 1000 // Store last 1000 updates
    }
}

data class LocationUpdate(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float
)

data class FrequentLocation(
    val latitude: Double,
    val longitude: Double,
    val visitCount: Int,
    val averageDuration: Long,
    val commonTimeRanges: List<TimeRange>,
    val lastVisit: Long
)

data class TimeRange(
    val start: Long,
    val end: Long
)

data class VisitFrequency(
    val frequency: Float, // 0-1
    val activities: List<String>
) 