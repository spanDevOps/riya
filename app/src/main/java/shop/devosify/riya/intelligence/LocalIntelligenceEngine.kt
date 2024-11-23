@Singleton
class LocalIntelligenceEngine @Inject constructor(
    private val gptRepository: GptRepository,
    private val networkUtils: NetworkUtils,
    private val memoryDao: MemoryDao,
    private val patternAnalysisService: PatternAnalysisService,
    private val analyticsService: AnalyticsService
) {
    suspend fun processQuery(input: String): LocalResponse {
        return try {
            if (networkUtils.isNetworkAvailable()) {
                // Online: Use GPT for best experience
                processWithGpt(input)
            } else {
                // Offline: Fallback to basic pattern matching
                processOffline(input)
            }
        } catch (e: Exception) {
            analyticsService.logError("query_processing", e.message ?: "Unknown error")
            LocalResponse.Error("Processing error: ${e.message}")
        }
    }

    private suspend fun processWithGpt(input: String): LocalResponse {
        // Get relevant context from local memory
        val context = memoryDao.getRelevantMemories(input, limit = 5)
        
        // Build rich context prompt
        val prompt = """
            Context from previous interactions:
            ${context.joinToString("\n")}
            
            Current input: $input
            
            Respond naturally and consider the context.
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response -> LocalResponse.Success(response) }
            .getOrElse { LocalResponse.Error(it.message ?: "GPT processing failed") }
    }

    private suspend fun processOffline(input: String): LocalResponse {
        // Simple pattern matching for essential commands
        val patterns = patternAnalysisService.findRelevantPatterns(input)
        return when {
            patterns.isNotEmpty() -> {
                val bestMatch = patterns.maxByOrNull { it.confidence }!!
                LocalResponse.Success(generatePatternResponse(bestMatch))
            }
            isBasicCommand(input) -> {
                handleBasicCommand(input)
            }
            else -> {
                LocalResponse.Error("I'm offline and can't process complex queries right now")
            }
        }
    }

    private fun isBasicCommand(input: String): Boolean {
        // Check for simple, predefined command patterns
        val basicPatterns = listOf(
            "turn (on|off).*",
            "set volume.*",
            "set brightness.*",
            "what('s| is) (the )?time.*",
            "battery (level|status).*"
        )
        return basicPatterns.any { pattern ->
            input.matches(Regex(pattern, RegexOption.IGNORE_CASE))
        }
    }
}

data class Intent(
    val type: IntentType,
    val slots: Map<String, String>
)

enum class IntentType {
    DEVICE_CONTROL,
    QUERY,
    ACTION,
    UNKNOWN
}

data class LocalContext(
    val timePatterns: List<UserPattern>,
    val relevantMemories: List<MemoryEntity>,
    val systemState: SystemState,
    val currentTime: LocalDateTime
)

sealed class LocalAction {
    data class DeviceControl(
        val device: String,
        val action: String,
        val confidence: Float
    ) : LocalAction()
    
    data class Query(
        val type: String,
        val parameters: Map<String, Any>
    ) : LocalAction()
    
    object Unknown : LocalAction()
}

sealed class LocalResponse {
    data class Success(val message: String) : LocalResponse()
    data class Error(val message: String) : LocalResponse()
    data class Unknown(val message: String) : LocalResponse()
} 