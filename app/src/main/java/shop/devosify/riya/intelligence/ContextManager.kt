@Singleton
class ContextManager @Inject constructor(
    private val memoryRetrievalService: MemoryRetrievalService,
    private val patternRecognitionEngine: PatternRecognitionEngine,
    private val emotionalContextService: EmotionalContextService,
    private val locationService: LocationService,
    private val systemMonitorService: SystemMonitorService,
    private val calendarService: CalendarIntegrationService,
    private val analyticsService: AnalyticsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _currentContext = MutableStateFlow<RiyaContext>(RiyaContext())
    val currentContext: StateFlow<RiyaContext> = _currentContext.asStateFlow()

    init {
        scope.launch {
            // Continuously update context by combining multiple data sources
            combine(
                emotionalContextService.getEmotionalProfile(),
                locationService.getCurrentLocation(),
                systemMonitorService.getSystemState(),
                calendarService.getUpcomingEvents(),
                patternRecognitionEngine.currentPatterns,
                memoryRetrievalService.getRecentMemories()
            ) { emotion, location, system, events, patterns, memories ->
                buildContext(
                    emotion = emotion,
                    location = location,
                    systemState = system,
                    events = events,
                    patterns = patterns,
                    memories = memories
                )
            }.collect { context ->
                updateContext(context)
            }
        }
    }

    private suspend fun buildContext(
        emotion: EmotionalState,
        location: Location,
        systemState: SystemState,
        events: List<CalendarEvent>,
        patterns: List<RecognizedPattern>,
        memories: List<MemoryEntity>
    ): RiyaContext {
        // Build rich context with confidence scores
        val timeContext = buildTimeContext(events, patterns)
        val locationContext = buildLocationContext(location, patterns)
        val emotionalContext = buildEmotionalContext(emotion, memories)
        val systemContext = buildSystemContext(systemState)
        val activityContext = buildActivityContext(patterns, memories)

        return RiyaContext(
            time = timeContext,
            location = locationContext,
            emotional = emotionalContext,
            system = systemContext,
            activity = activityContext,
            confidence = calculateContextConfidence(
                timeContext, locationContext, emotionalContext,
                systemContext, activityContext
            )
        )
    }

    private suspend fun buildTimeContext(
        events: List<CalendarEvent>,
        patterns: List<RecognizedPattern>
    ): TimeContext {
        val now = System.currentTimeMillis()
        val upcomingEvent = events.firstOrNull { it.startTime > now }
        val relevantPatterns = patterns.filter { it.type == PatternType.ROUTINE }

        return TimeContext(
            currentTime = now,
            timeOfDay = getTimeOfDay(now),
            upcomingEvent = upcomingEvent,
            routinePatterns = relevantPatterns,
            isUsualActivityTime = isUsualActivityTime(now, patterns),
            confidence = calculateTimeContextConfidence(upcomingEvent, relevantPatterns)
        )
    }

    private suspend fun buildLocationContext(
        location: Location,
        patterns: List<RecognizedPattern>
    ): LocationContext {
        val locationPatterns = patterns.filter { it.type == PatternType.LOCATION }
        val knownLocations = memoryRetrievalService.getKnownLocations()
        
        return LocationContext(
            currentLocation = location,
            isKnownLocation = knownLocations.any { it.matches(location) },
            locationPatterns = locationPatterns,
            nearbyPlaces = locationService.getNearbyPlaces(location),
            confidence = calculateLocationConfidence(location, knownLocations)
        )
    }

    private suspend fun buildEmotionalContext(
        emotion: EmotionalState,
        memories: List<MemoryEntity>
    ): EmotionalContext {
        val recentEmotionalMemories = memories
            .filter { it.type == MemoryType.EMOTIONAL }
            .takeLast(5)

        return EmotionalContext(
            currentEmotion = emotion,
            recentEmotions = recentEmotionalMemories,
            emotionalTrend = analyzeEmotionalTrend(recentEmotionalMemories),
            confidence = calculateEmotionalConfidence(emotion, recentEmotionalMemories)
        )
    }

    private fun buildSystemContext(state: SystemState): SystemContext {
        return SystemContext(
            batteryLevel = state.batteryLevel,
            isCharging = state.isCharging,
            availableMemory = state.availableMemory,
            cpuUsage = state.cpuUsage,
            networkType = state.networkType,
            isDoNotDisturb = state.isDoNotDisturb,
            confidence = 1.0f // System state is always accurate
        )
    }

    private suspend fun buildActivityContext(
        patterns: List<RecognizedPattern>,
        memories: List<MemoryEntity>
    ): ActivityContext {
        val currentActivity = determineCurrentActivity(patterns, memories)
        val routineMatch = findMatchingRoutine(currentActivity, patterns)
        
        return ActivityContext(
            currentActivity = currentActivity,
            matchingRoutine = routineMatch,
            isUsualActivity = routineMatch != null,
            relatedMemories = findRelatedMemories(currentActivity, memories),
            confidence = calculateActivityConfidence(currentActivity, routineMatch)
        )
    }

    private fun calculateContextConfidence(vararg contexts: ContextComponent): Float {
        // Weight and combine confidence scores
        val weights = mapOf(
            TimeContext::class to 0.2f,
            LocationContext::class to 0.2f,
            EmotionalContext::class to 0.2f,
            SystemContext::class to 0.1f,
            ActivityContext::class to 0.3f
        )

        return contexts.sumOf { context ->
            (weights[context::class] ?: 0f) * context.confidence
        }.toFloat()
    }

    private fun updateContext(newContext: RiyaContext) {
        _currentContext.value = newContext
        
        // Log significant context changes
        if (hasSignificantChange(newContext, _currentContext.value)) {
            analyticsService.logEvent("context_changed", mapOf(
                "confidence" to newContext.confidence,
                "components" to newContext.getChangedComponents(_currentContext.value)
            ))
        }
    }

    companion object {
        private const val SIGNIFICANT_CHANGE_THRESHOLD = 0.3f
    }
}

interface ContextComponent {
    val confidence: Float
}

data class RiyaContext(
    val time: TimeContext? = null,
    val location: LocationContext? = null,
    val emotional: EmotionalContext? = null,
    val system: SystemContext? = null,
    val activity: ActivityContext? = null,
    val confidence: Float = 0f
)

data class TimeContext(
    val currentTime: Long,
    val timeOfDay: TimeOfDay,
    val upcomingEvent: CalendarEvent?,
    val routinePatterns: List<RecognizedPattern>,
    val isUsualActivityTime: Boolean,
    override val confidence: Float
) : ContextComponent

// Other context data classes follow similar pattern... 