@Singleton
class PlacesService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gptRepository: GptRepository,
    private val analyticsService: AnalyticsService
) {
    private val placesClient = Places.createClient(context)
    private val _placeHistory = MutableStateFlow<List<PlaceVisit>>(emptyList())
    private val _currentPlace = MutableStateFlow<Place?>(null)

    suspend fun getCurrentPlace(location: Location): Flow<Place> = flow {
        try {
            val placeFields = listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.TYPES,
                Place.Field.BUSINESS_STATUS,
                Place.Field.OPENING_HOURS,
                Place.Field.RATING
            )

            val request = FindCurrentPlaceRequest.newInstance(placeFields)
            val response = placesClient.findCurrentPlace(request).await()
            
            response.placeLikelihoods.maxByOrNull { it.likelihood }?.place?.let { place ->
                _currentPlace.value = place
                recordPlaceVisit(place)
                emit(place)
            }
        } catch (e: Exception) {
            analyticsService.logError("places_detection", e.message ?: "Unknown error")
            emit(createUnknownPlace(location))
        }
    }

    suspend fun getPlaceContext(place: Place): PlaceContext {
        val visits = _placeHistory.value.filter { it.placeId == place.id }
        val patterns = analyzePlacePatterns(visits)
        
        return PlaceContext(
            place = place,
            visitFrequency = calculateVisitFrequency(visits),
            commonActivities = patterns.activities,
            typicalDuration = patterns.averageDuration,
            commonTimeRanges = patterns.timeRanges,
            relevantMemories = findRelevantMemories(place)
        )
    }

    private suspend fun analyzePlacePatterns(visits: List<PlaceVisit>): PlacePatterns {
        val prompt = """
            Analyze these place visits and identify patterns:
            Visits: ${visits.joinToString("\n") { 
                "${it.timestamp}: Duration=${it.duration}ms" 
            }}
            
            Return format: JSON with:
            - activities: list of common activities
            - averageDuration: typical visit duration in ms
            - timeRanges: list of common visit times
            - significance: importance score 0-1
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, PlacePatterns::class.java)
            }
            .getOrDefault(PlacePatterns.default())
    }

    private fun recordPlaceVisit(place: Place) {
        val visits = _placeHistory.value.toMutableList()
        visits.add(
            PlaceVisit(
                placeId = place.id!!,
                placeName = place.name!!,
                timestamp = System.currentTimeMillis(),
                duration = 0L // Will be updated on exit
            )
        )
        if (visits.size > MAX_HISTORY_SIZE) {
            visits.removeAt(0)
        }
        _placeHistory.value = visits
    }

    private fun calculateVisitFrequency(visits: List<PlaceVisit>): Float {
        if (visits.isEmpty()) return 0f
        
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        val recentVisits = visits.count { it.timestamp > oneWeekAgo }
        
        return (recentVisits / 7f).coerceIn(0f, 1f)
    }

    private suspend fun findRelevantMemories(place: Place): List<MemoryEntity> {
        // Implementation to find memories related to this place
        return emptyList() // Placeholder
    }

    private fun createUnknownPlace(location: Location): Place {
        return Place.builder()
            .setLatLng(LatLng(location.latitude, location.longitude))
            .setName("Unknown Location")
            .setAddress("${location.latitude}, ${location.longitude}")
            .build()
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 100
    }
}

data class PlaceVisit(
    val placeId: String,
    val placeName: String,
    val timestamp: Long,
    val duration: Long
)

data class PlaceContext(
    val place: Place,
    val visitFrequency: Float,
    val commonActivities: List<String>,
    val typicalDuration: Long,
    val commonTimeRanges: List<TimeRange>,
    val relevantMemories: List<MemoryEntity>
)

data class PlacePatterns(
    val activities: List<String>,
    val averageDuration: Long,
    val timeRanges: List<TimeRange>,
    val significance: Float
) {
    companion object {
        fun default() = PlacePatterns(
            activities = emptyList(),
            averageDuration = 0L,
            timeRanges = emptyList(),
            significance = 0f
        )
    }
} 