@Singleton
class TravelAssistantService @Inject constructor(
    private val locationService: LocationService,
    private val calendarService: CalendarService,
    private val weatherService: WeatherService,
    private val trafficService: TrafficService
) {
    suspend fun planCommute() {
        // Check calendar for meetings
        val nextMeeting = calendarService.getNextMeeting()
        if (nextMeeting != null) {
            // Check traffic and weather
            val trafficConditions = trafficService.checkRoute(nextMeeting.location)
            val weather = weatherService.getForecast()
            
            // Suggest optimal departure time
            suggestDepartureTime(nextMeeting, trafficConditions, weather)
        }
    }

    suspend fun suggestParking() {
        val destination = locationService.getDestination()
        val parkingSpots = findAvailableParking(destination)
        suggestBestParkingSpot(parkingSpots)
    }

    private suspend fun suggestAlternativeRoutes() {
        val usualRoute = getUserUsualRoute()
        if (hasTrafficIssues(usualRoute)) {
            suggestBetterRoute()
        }
    }
} 