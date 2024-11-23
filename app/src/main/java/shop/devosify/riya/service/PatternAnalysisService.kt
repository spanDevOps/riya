@Singleton
class PatternAnalysisService @Inject constructor(
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository,
    private val systemMonitorService: SystemMonitorService,
    private val calendarService: CalendarIntegrationService,
    private val emotionalContextService: EmotionalContextService,
    private val analyticsService: AnalyticsService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _patterns = MutableStateFlow<List<UserPattern>>(emptyList())
    val patterns: StateFlow<List<UserPattern>> = _patterns.asStateFlow()

    init {
        scope.launch {
            // Periodically analyze patterns
            while (true) {
                try {
                    analyzeAndUpdatePatterns()
                    delay(PATTERN_UPDATE_INTERVAL)
                } catch (e: Exception) {
                    analyticsService.logError("pattern_analysis", e.message ?: "Unknown error")
                    delay(ERROR_RETRY_DELAY)
                }
            }
        }
    }

    suspend fun analyzeAndUpdatePatterns() {
        combine(
            getDailyRoutines(),
            getLocationPatterns(),
            getDeviceUsagePatterns(),
            getEmotionalPatterns(),
            getSocialPatterns()
        ) { routines, locations, deviceUsage, emotions, social ->
            val allPatterns = mutableListOf<UserPattern>()
            allPatterns.addAll(routines)
            allPatterns.addAll(locations)
            allPatterns.addAll(deviceUsage)
            allPatterns.addAll(emotions)
            allPatterns.addAll(social)
            allPatterns
        }.collect { patterns ->
            _patterns.value = patterns
        }
    }

    suspend fun getDailyRoutines(): Flow<List<UserPattern>> = flow {
        val memories = memoryDao.getMemoriesByType(MemoryType.ROUTINE)
        val calendar = calendarService.getUpcomingEvents().first()
        
        val prompt = """
            Analyze these daily activities and calendar events:
            Memories: ${memories.joinToString("\n")}
            Calendar: ${calendar.joinToString("\n")}
            
            Identify:
            1. Regular daily patterns
            2. Weekly routines
            3. Time preferences
            4. Activity clusters
            
            Return: JSON array of patterns with:
            - type: ROUTINE
            - description: pattern description
            - confidence: 0-1
            - timeContext: {
                timeOfDay: [hours],
                daysOfWeek: [days],
                frequency: "daily/weekly/monthly"
              }
        """.trimIndent()

        val routines = gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<UserPattern>::class.java).toList()
            }
            .getOrDefault(emptyList())
            .filter { it.confidence > CONFIDENCE_THRESHOLD }

        emit(routines)
    }

    suspend fun getDeviceUsagePatterns(): Flow<List<UserPattern>> = flow {
        val systemState = systemMonitorService.getCurrentState()
        val usageMemories = memoryDao.getMemoriesByTag("device_usage")
        
        val prompt = """
            Analyze device usage patterns:
            System State: $systemState
            Usage History: ${usageMemories.joinToString("\n")}
            
            Identify:
            1. App usage patterns
            2. Feature preferences
            3. Usage times
            4. Battery consumption patterns
            
            Return: JSON array of patterns with:
            - type: HABIT
            - description: usage pattern
            - confidence: 0-1
            - timeContext: timing details
        """.trimIndent()

        val patterns = gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<UserPattern>::class.java).toList()
            }
            .getOrDefault(emptyList())
            .filter { it.confidence > CONFIDENCE_THRESHOLD }

        emit(patterns)
    }

    suspend fun getEmotionalPatterns(): Flow<List<UserPattern>> = flow {
        val emotionalProfile = emotionalContextService.getEmotionalProfile()
        val emotionMemories = memoryDao.getMemoriesByTag("emotion")
        
        val prompt = """
            Analyze emotional patterns:
            Current Profile: $emotionalProfile
            History: ${emotionMemories.joinToString("\n")}
            
            Identify:
            1. Emotional triggers
            2. Mood patterns
            3. Response preferences
            4. Interaction styles
            
            Return: JSON array of patterns with:
            - type: PREFERENCE
            - description: emotional pattern
            - confidence: 0-1
            - context: emotional context
        """.trimIndent()

        val patterns = gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<UserPattern>::class.java).toList()
            }
            .getOrDefault(emptyList())
            .filter { it.confidence > CONFIDENCE_THRESHOLD }

        emit(patterns)
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.7f
        private const val PATTERN_UPDATE_INTERVAL = 3600000L // 1 hour
        private const val ERROR_RETRY_DELAY = 300000L // 5 minutes
    }
}

data class UserPattern(
    val type: PatternType,
    val description: String,
    val confidence: Float,
    val suggestions: List<String>,
    val timeContext: TimeContext? = null,
    val locationContext: LocationContext? = null
)

enum class PatternType {
    ROUTINE,    // Daily/weekly patterns
    PREFERENCE, // User preferences
    HABIT,      // Repeated behaviors
    SOCIAL      // Interaction patterns
}

data class TimeContext(
    val timeOfDay: List<Int>, // Hours of day
    val daysOfWeek: List<Int>, // Days of week
    val frequency: String // "daily", "weekly", "monthly"
)

data class LocationContext(
    val location: String,
    val frequency: Float, // 0-1
    val associatedActivities: List<String>
) 