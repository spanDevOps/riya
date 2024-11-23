@Singleton
class InformationService @Inject constructor(
    private val newsApiService: NewsApiService,
    private val weatherApiService: WeatherApiService,
    private val locationService: LocationService,
    private val trafficApiService: TrafficApiService,
    private val userPreferencesManager: UserPreferencesManager
) {
    suspend fun getNews(category: String? = null): List<NewsItem> {
        val userInterests = userPreferencesManager.getUserInterests()
        val location = locationService.getCurrentLocation()
        
        return newsApiService.getNews(
            categories = category?.let { listOf(it) } ?: userInterests,
            location = location,
            language = userPreferencesManager.getPreferredLanguage()
        )
    }

    suspend fun getWeather(): WeatherInfo {
        val location = locationService.getCurrentLocation()
        return weatherApiService.getWeather(location)
    }

    suspend fun checkRouteStatus(routeName: String = "work"): RouteStatus {
        val route = when (routeName) {
            "work" -> userPreferencesManager.getWorkRoute()
            "home" -> userPreferencesManager.getHomeRoute()
            else -> throw IllegalArgumentException("Unknown route")
        }
        
        return trafficApiService.getRouteStatus(route)
    }
} 