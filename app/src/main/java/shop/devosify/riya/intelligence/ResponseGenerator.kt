@Singleton
class ResponseGenerator @Inject constructor(
    private val contextManager: ContextManager,
    private val modelManager: ModelManager,
    private val resourceAwareDecisionEngine: ResourceAwareDecisionEngine,
    private val memoryRetrievalService: MemoryRetrievalService,
    private val patternAnalysisService: PatternAnalysisService,
    private val emotionalContextService: EmotionalContextService,
    private val analyticsService: AnalyticsService
) {
    suspend fun generateResponse(input: String): Response {
        val context = contextManager.currentContext.first()
        
        return when {
            // Handle system commands first
            isSystemCommand(input) -> handleSystemCommand(input, context)
            
            // Check if we should use ML
            resourceAwareDecisionEngine.shouldUseMlModel(ModelType.NLU) -> {
                generateMlResponse(input, context)
            }
            
            // Fallback to heuristic response
            else -> generateHeuristicResponse(input, context)
        }
    }

    private suspend fun generateMlResponse(
        input: String, 
        context: RiyaContext
    ): Response {
        val nluModel = modelManager.getModel(ModelType.NLU) ?: 
            return generateHeuristicResponse(input, context)

        // Build rich context prompt
        val prompt = buildString {
            append("Current context:\n")
            append("Time: ${context.time?.timeOfDay}\n")
            append("Location: ${context.location?.currentLocation}\n")
            append("Emotion: ${context.emotional?.currentEmotion}\n")
            append("Activity: ${context.activity?.currentActivity}\n")
            append("System: ${context.system?.toString()}\n\n")
            
            append("User input: $input\n\n")
            
            append("Generate a response that is:\n")
            append("1. Contextually appropriate\n")
            append("2. Emotionally aware\n")
            append("3. Actionable if needed\n")
            append("4. Concise but helpful\n")
        }

        return try {
            val embedding = nluModel.generateEmbedding(prompt)
            val relevantMemories = memoryRetrievalService.findSimilarMemories(embedding)
            val patterns = patternAnalysisService.findRelevantPatterns(input, context)
            
            Response(
                text = nluModel.generateResponse(prompt, relevantMemories, patterns),
                confidence = calculateConfidence(context, relevantMemories, patterns),
                source = ResponseSource.ML,
                context = context
            )
        } catch (e: Exception) {
            analyticsService.logError("ml_response_generation", e.message ?: "Unknown error")
            generateHeuristicResponse(input, context)
        }
    }

    private suspend fun generateHeuristicResponse(
        input: String,
        context: RiyaContext
    ): Response {
        // Use pattern matching and rules for response generation
        val patterns = patternAnalysisService.findRelevantPatterns(input, context)
        val matchedPattern = patterns.maxByOrNull { it.confidence }
        
        val response = when {
            matchedPattern != null -> {
                generatePatternBasedResponse(matchedPattern, context)
            }
            isQuestion(input) -> {
                generateFactualResponse(input, context)
            }
            isEmotional(input) -> {
                generateEmotionalResponse(input, context)
            }
            else -> {
                generateFallbackResponse(input, context)
            }
        }

        return Response(
            text = response,
            confidence = calculateHeuristicConfidence(input, context),
            source = ResponseSource.HEURISTIC,
            context = context
        )
    }

    private suspend fun generatePatternBasedResponse(
        pattern: RecognizedPattern,
        context: RiyaContext
    ): String {
        return when (pattern.type) {
            PatternType.COMMAND -> executeCommand(pattern)
            PatternType.PREFERENCE -> suggestBasedOnPreference(pattern)
            PatternType.ROUTINE -> handleRoutinePattern(pattern)
            PatternType.EMOTION -> generateEmpatheticResponse(pattern)
            PatternType.CONTEXT -> handleContextualPattern(pattern)
        }
    }

    private fun calculateConfidence(
        context: RiyaContext,
        memories: List<MemoryEntity>,
        patterns: List<RecognizedPattern>
    ): Float {
        var confidence = context.confidence
        
        // Boost confidence based on memory matches
        if (memories.isNotEmpty()) {
            confidence += MEMORY_CONFIDENCE_BOOST * 
                (memories.sumOf { it.confidence.toDouble() } / memories.size).toFloat()
        }
        
        // Boost confidence based on pattern matches
        if (patterns.isNotEmpty()) {
            confidence += PATTERN_CONFIDENCE_BOOST * 
                (patterns.maxOf { it.confidence })
        }
        
        return confidence.coerceIn(0f, 1f)
    }

    private fun calculateHeuristicConfidence(
        input: String,
        context: RiyaContext
    ): Float {
        var confidence = BASE_HEURISTIC_CONFIDENCE
        
        // Adjust based on input clarity
        confidence += if (isWellFormedInput(input)) CLARITY_BOOST else 0f
        
        // Adjust based on context quality
        confidence += context.confidence * CONTEXT_WEIGHT
        
        // Penalize for complex queries
        confidence -= complexityPenalty(input)
        
        return confidence.coerceIn(0f, 1f)
    }

    companion object {
        private const val BASE_HEURISTIC_CONFIDENCE = 0.5f
        private const val MEMORY_CONFIDENCE_BOOST = 0.2f
        private const val PATTERN_CONFIDENCE_BOOST = 0.3f
        private const val CLARITY_BOOST = 0.1f
        private const val CONTEXT_WEIGHT = 0.2f
    }
}

data class Response(
    val text: String,
    val confidence: Float,
    val source: ResponseSource,
    val context: RiyaContext,
    val action: ResponseAction? = null
)

enum class ResponseSource {
    ML,
    HEURISTIC,
    HYBRID
}

sealed class ResponseAction {
    data class SystemCommand(val command: String) : ResponseAction()
    data class Notification(val message: String) : ResponseAction()
    data class Automation(val rule: AutomationRule) : ResponseAction()
} 