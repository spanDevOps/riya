@Singleton
class ProactiveAssistanceService @Inject constructor(
    private val memoryRetrievalService: MemoryRetrievalService,
    private val systemMonitorService: SystemMonitorService,
    private val calendarService: CalendarService,
    private val locationService: LocationService,
    private val patternAnalysisService: PatternAnalysisService,
    private val emotionalContextService: EmotionalContextService,
    private val gptRepository: GptRepository,
    private val analyticsService: AnalyticsService
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val _suggestions = MutableStateFlow<List<ProactiveSuggestion>>(emptyList())
    val suggestions: StateFlow<List<ProactiveSuggestion>> = _suggestions.asStateFlow()

    fun startProactiveMonitoring() {
        scope.launch {
            // Combine multiple data streams for rich context
            combine(
                systemMonitorService.getSystemState(),
                locationService.getCurrentLocation(),
                calendarService.getUpcomingEvents(),
                patternAnalysisService.getUserPatterns(),
                memoryRetrievalService.getRecentMemories(),
                emotionalContextService.getEmotionalProfile(),
                patternAnalysisService.getDailyRoutines()
            ) { systemState, location, events, patterns, memories, emotion, routines ->
                generateContextAwareSuggestions(
                    systemState, location, events, 
                    patterns, memories, emotion, routines
                )
            }.collect { suggestions ->
                processSuggestions(suggestions)
            }
        }
    }

    private suspend fun generateContextAwareSuggestions(
        systemState: SystemState,
        location: Location,
        events: List<CalendarEvent>,
        patterns: List<UserPattern>,
        memories: List<MemoryEntity>,
        emotion: EmotionalProfile,
        routines: List<DailyRoutine>
    ): List<ProactiveSuggestion> {
        // Build rich context for GPT
        val context = buildContext(
            systemState, location, events, 
            patterns, memories, emotion, routines
        )

        val prompt = """
            Analyze this context and generate proactive suggestions:
            
            Current Context:
            ${context.toDetailedString()}
            
            Consider:
            1. User's emotional state and preferences
            2. Time-based patterns and routines
            3. Location context and travel time
            4. Device state and battery level
            5. Calendar commitments
            6. Past behaviors and memories
            
            Generate suggestions that are:
            - Timely and relevant
            - Emotionally appropriate
            - Actionable and specific
            - Prioritized by importance
            - Contextually aware
            
            Return format: JSON array of suggestions with:
            - type: REMINDER, RECOMMENDATION, ALERT, OPTIMIZATION
            - priority: 1-5
            - content: suggestion text
            - timing: when to present
            - context: why this suggestion is relevant
            - confidence: 0-1
            - category: ROUTINE, PRODUCTIVITY, WELLBEING, SOCIAL
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<ProactiveSuggestion>::class.java).toList()
            }
            .getOrDefault(emptyList())
            .filter { it.confidence > CONFIDENCE_THRESHOLD }
            .sortedByDescending { it.priority }
    }

    private suspend fun processSuggestions(suggestions: List<ProactiveSuggestion>) {
        val filteredSuggestions = suggestions
            .filter { suggestion ->
                // Filter out irrelevant or low-confidence suggestions
                isRelevantSuggestion(suggestion) &&
                !isRedundant(suggestion) &&
                meetsUserPreferences(suggestion)
            }
            .take(MAX_ACTIVE_SUGGESTIONS)

        _suggestions.value = filteredSuggestions
        
        // Log for analytics
        analyticsService.logSuggestions(
            generated = suggestions.size,
            filtered = filteredSuggestions.size,
            categories = filteredSuggestions.groupBy { it.category }
        )
    }

    private suspend fun isRelevantSuggestion(suggestion: ProactiveSuggestion): Boolean {
        // Check if the suggestion timing is appropriate
        val now = System.currentTimeMillis()
        if (suggestion.timing < now) return false

        // Check if the suggestion matches current context
        val currentContext = systemMonitorService.getCurrentState()
        return when (suggestion.type) {
            SuggestionType.REMINDER -> isTimingAppropriate(suggestion, currentContext)
            SuggestionType.RECOMMENDATION -> matchesUserPreferences(suggestion)
            SuggestionType.ALERT -> isUrgentAndRelevant(suggestion, currentContext)
            SuggestionType.OPTIMIZATION -> canOptimize(suggestion, currentContext)
        }
    }

    private suspend fun isRedundant(suggestion: ProactiveSuggestion): Boolean {
        // Check against recent suggestions
        val recentSuggestions = _suggestions.value
        return recentSuggestions.any { recent ->
            recent.content.similarityTo(suggestion.content) > SIMILARITY_THRESHOLD
        }
    }

    private suspend fun meetsUserPreferences(suggestion: ProactiveSuggestion): Boolean {
        val userPreferences = memoryRetrievalService.getUserPreferences()
        return when (suggestion.category) {
            SuggestionCategory.ROUTINE -> userPreferences.enableRoutineReminders
            SuggestionCategory.PRODUCTIVITY -> userPreferences.enableProductivitySuggestions
            SuggestionCategory.WELLBEING -> userPreferences.enableWellbeingSuggestions
            SuggestionCategory.SOCIAL -> userPreferences.enableSocialSuggestions
        }
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.7f
        private const val SIMILARITY_THRESHOLD = 0.8f
        private const val MAX_ACTIVE_SUGGESTIONS = 5
    }
}

data class ProactiveSuggestion(
    val type: SuggestionType,
    val priority: Int,
    val content: String,
    val timing: Long,
    val context: String,
    val confidence: Float,
    val category: SuggestionCategory,
    val action: SuggestionAction? = null
)

enum class SuggestionType {
    REMINDER,
    RECOMMENDATION,
    ALERT,
    OPTIMIZATION
}

enum class SuggestionCategory {
    ROUTINE,
    PRODUCTIVITY,
    WELLBEING,
    SOCIAL
}

data class SuggestionAction(
    val type: ActionType,
    val data: Map<String, Any>
)

enum class ActionType {
    OPEN_APP,
    SCHEDULE_EVENT,
    MODIFY_SETTING,
    START_ROUTINE
} 