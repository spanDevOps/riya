@Singleton
class PatternRecognitionEngine @Inject constructor(
    private val memoryDao: MemoryDao,
    private val resourceAwareDecisionEngine: ResourceAwareDecisionEngine,
    private val analyticsService: AnalyticsService
) {
    private val patternCache = LRUCache<String, RecognizedPattern>(100)
    private val mlPatternDetector = MlPatternDetector()
    private val heuristicPatternDetector = HeuristicPatternDetector()

    suspend fun recognizePatterns(
        input: String,
        context: Map<String, Any>
    ): List<RecognizedPattern> {
        return try {
            when (resourceAwareDecisionEngine.currentMode.value) {
                ProcessingMode.ML_ONLY -> recognizeWithMl(input, context)
                ProcessingMode.HEURISTICS -> recognizeWithHeuristics(input, context)
                ProcessingMode.HYBRID -> {
                    val mlPatterns = recognizeWithMl(input, context)
                    val heuristicPatterns = recognizeWithHeuristics(input, context)
                    mergePatterns(mlPatterns, heuristicPatterns)
                }
            }
        } catch (e: Exception) {
            analyticsService.logError("pattern_recognition", e.message ?: "Unknown error")
            emptyList()
        }
    }

    private suspend fun recognizeWithMl(
        input: String,
        context: Map<String, Any>
    ): List<RecognizedPattern> {
        // Get relevant memories
        val memories = memoryDao.getRelevantMemories(input, limit = 100)
        
        // Generate embeddings
        val inputEmbedding = mlPatternDetector.generateEmbedding(input)
        val contextEmbedding = mlPatternDetector.generateContextEmbedding(context)
        
        // Find similar patterns
        val similarPatterns = mlPatternDetector.findSimilarPatterns(
            inputEmbedding,
            contextEmbedding,
            memories
        )
        
        // Score and filter patterns
        return similarPatterns
            .map { pattern ->
                val confidence = calculatePatternConfidence(
                    pattern,
                    input,
                    context,
                    memories
                )
                pattern.copy(confidence = confidence)
            }
            .filter { it.confidence > CONFIDENCE_THRESHOLD }
            .sortedByDescending { it.confidence }
    }

    private suspend fun recognizeWithHeuristics(
        input: String,
        context: Map<String, Any>
    ): List<RecognizedPattern> {
        // Apply rule-based pattern matching
        val matchedRules = heuristicPatternDetector.matchRules(input)
        
        // Check context patterns
        val contextPatterns = heuristicPatternDetector.findContextPatterns(context)
        
        // Combine and score patterns
        return (matchedRules + contextPatterns)
            .map { pattern ->
                val confidence = calculateHeuristicConfidence(
                    pattern,
                    input,
                    context
                )
                pattern.copy(confidence = confidence)
            }
            .filter { it.confidence > CONFIDENCE_THRESHOLD }
            .sortedByDescending { it.confidence }
    }

    private fun mergePatterns(
        mlPatterns: List<RecognizedPattern>,
        heuristicPatterns: List<RecognizedPattern>
    ): List<RecognizedPattern> {
        val allPatterns = mutableListOf<RecognizedPattern>()
        
        // Add ML patterns with higher confidence
        allPatterns.addAll(
            mlPatterns.filter { mlPattern ->
                val heuristicConfidence = heuristicPatterns
                    .find { it.type == mlPattern.type }
                    ?.confidence ?: 0f
                mlPattern.confidence > heuristicConfidence
            }
        )
        
        // Add heuristic patterns with higher confidence
        allPatterns.addAll(
            heuristicPatterns.filter { hPattern ->
                val mlConfidence = mlPatterns
                    .find { it.type == hPattern.type }
                    ?.confidence ?: 0f
                hPattern.confidence > mlConfidence
            }
        )
        
        return allPatterns.sortedByDescending { it.confidence }
    }

    companion object {
        private const val CONFIDENCE_THRESHOLD = 0.6f
    }
}

data class RecognizedPattern(
    val type: PatternType,
    val description: String,
    val confidence: Float,
    val metadata: Map<String, Any>,
    val actionable: Boolean = false
)

enum class PatternType {
    COMMAND,
    PREFERENCE,
    ROUTINE,
    EMOTION,
    CONTEXT
} 