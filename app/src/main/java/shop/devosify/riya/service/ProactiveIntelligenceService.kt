@Singleton
class ProactiveIntelligenceService @Inject constructor(
    private val memoryRetrievalService: MemoryRetrievalService,
    private val patternAnalysisService: PatternAnalysisService,
    private val systemMonitorService: SystemMonitorService,
    private val calendarIntegrationService: CalendarIntegrationService,
    private val emotionalContextService: EmotionalContextService,
    private val gptRepository: GptRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    fun startProactiveMonitoring() {
        scope.launch {
            combine(
                systemMonitorService.getSystemState(),
                patternAnalysisService.analyzePatterns(),
                calendarIntegrationService.getUpcomingEvents(),
                emotionalContextService.getEmotionalProfile(),
                memoryRetrievalService.getRecentMemories()
            ) { systemState, patterns, events, emotion, memories ->
                generateProactiveSuggestions(
                    systemState, patterns, events, emotion, memories
                )
            }.collect { suggestions ->
                processSuggestions(suggestions)
            }
        }
    }

    private suspend fun generateProactiveSuggestions(
        systemState: SystemState,
        patterns: List<UserPattern>,
        events: List<CalendarEvent>,
        emotion: EmotionalProfile,
        memories: List<MemoryEntity>
    ): List<ProactiveSuggestion> {
        val context = buildContext(
            systemState, patterns, events, emotion, memories
        )
        
        val prompt = """
            Analyze this context and generate proactive suggestions:
            System State: $systemState
            Patterns: ${patterns.joinToString("\n")}
            Events: ${events.joinToString("\n")}
            Emotional State: $emotion
            Recent Memories: ${memories.joinToString("\n")}
            
            Generate suggestions that are:
            1. Timely and relevant
            2. Emotionally appropriate
            3. Actionable
            4. Based on past patterns
            5. Considerate of current context
            
            Return format: JSON array of suggestions with:
            - type: REMINDER, RECOMMENDATION, ALERT, OPTIMIZATION
            - priority: 1-5
            - content: suggestion text
            - timing: when to present
            - context: why this suggestion is relevant
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<ProactiveSuggestion>::class.java).toList()
            }
            .getOrDefault(emptyList())
    }

    private suspend fun processSuggestions(suggestions: List<ProactiveSuggestion>) {
        suggestions.forEach { suggestion ->
            when (suggestion.type) {
                SuggestionType.REMINDER -> scheduleReminder(suggestion)
                SuggestionType.RECOMMENDATION -> showRecommendation(suggestion)
                SuggestionType.ALERT -> showAlert(suggestion)
                SuggestionType.OPTIMIZATION -> applyOptimization(suggestion)
            }
        }
    }
}

data class ProactiveSuggestion(
    val type: SuggestionType,
    val priority: Int,
    val content: String,
    val timing: Long,
    val context: String
)

enum class SuggestionType {
    REMINDER,
    RECOMMENDATION,
    ALERT,
    OPTIMIZATION
} 