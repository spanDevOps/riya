@Singleton
class MemoryRetrievalEngine @Inject constructor(
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository,
    private val contextManager: ContextManager,
    private val analyticsService: AnalyticsService
) {
    private val memoryCache = LRUCache<String, List<MemoryEntity>>(100)
    private val embeddingCache = LRUCache<String, FloatArray>(1000)

    suspend fun findRelevantMemories(
        query: String,
        limit: Int = 5,
        minConfidence: Float = 0.7f
    ): List<MemoryEntity> {
        try {
            // Check cache first
            memoryCache.get(query)?.let { cached ->
                analyticsService.logEvent("memory_cache_hit")
                return cached
            }

            // Get current context
            val context = contextManager.currentContext.first()
            
            // Generate embedding for query
            val queryEmbedding = generateQueryEmbedding(query)
            
            // Build search criteria
            val searchCriteria = buildSearchCriteria(query, context)
            
            // Get candidate memories
            val candidates = memoryDao.searchMemories(searchCriteria)
            
            // Score and rank memories
            val rankedMemories = candidates.map { memory ->
                val score = calculateRelevanceScore(
                    memory = memory,
                    queryEmbedding = queryEmbedding,
                    context = context,
                    query = query
                )
                memory to score
            }
            .filter { it.second >= minConfidence }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(limit)

            // Cache results
            memoryCache.put(query, rankedMemories)
            
            return rankedMemories

        } catch (e: Exception) {
            analyticsService.logError("memory_retrieval", e.message ?: "Unknown error")
            return emptyList()
        }
    }

    private suspend fun generateQueryEmbedding(query: String): FloatArray {
        return embeddingCache.get(query) ?: run {
            val prompt = """
                Generate a semantic embedding for this query:
                $query
                
                Consider:
                1. Core concepts and entities
                2. Intent and context
                3. Temporal aspects
                4. Emotional content
                
                Return: JSON array of 128 floating point values
            """.trimIndent()

            gptRepository.generateText(prompt)
                .map { response -> parseEmbedding(response) }
                .getOrDefault(FloatArray(128))
                .also { embedding ->
                    embeddingCache.put(query, embedding)
                }
        }
    }

    private fun calculateRelevanceScore(
        memory: MemoryEntity,
        queryEmbedding: FloatArray,
        context: RiyaContext,
        query: String
    ): Float {
        // Semantic similarity (40%)
        val semanticScore = cosineSimilarity(queryEmbedding, memory.embedding) * 0.4f
        
        // Temporal relevance (20%)
        val temporalScore = calculateTemporalScore(memory, context.time) * 0.2f
        
        // Contextual match (20%)
        val contextScore = calculateContextScore(memory, context) * 0.2f
        
        // Importance weight (20%)
        val importanceScore = (memory.importance / 5f) * 0.2f
        
        return (semanticScore + temporalScore + contextScore + importanceScore)
            .coerceIn(0f, 1f)
    }

    private fun calculateTemporalScore(memory: MemoryEntity, timeContext: TimeContext?): Float {
        if (timeContext == null) return 0.5f
        
        val age = System.currentTimeMillis() - memory.timestamp
        val recency = 1f - (age.toFloat() / MAX_MEMORY_AGE).coerceIn(0f, 1f)
        
        // Check if memory matches current time patterns
        val timePatternMatch = timeContext.routinePatterns.any { pattern ->
            memory.context?.get("timeOfDay") == pattern.timeContext?.timeOfDay
        }

        return (recency * 0.7f + (if (timePatternMatch) 0.3f else 0f))
            .coerceIn(0f, 1f)
    }

    private fun calculateContextScore(memory: MemoryEntity, context: RiyaContext): Float {
        var score = 0f
        var factors = 0
        
        // Location match
        context.location?.let { locationContext ->
            score += if (memory.context?.get("location") == locationContext.currentLocation) 1f else 0f
            factors++
        }
        
        // Emotional context match
        context.emotional?.let { emotionalContext ->
            score += if (memory.context?.get("emotion") == emotionalContext.currentEmotion) 1f else 0f
            factors++
        }
        
        // Activity match
        context.activity?.let { activityContext ->
            score += if (memory.context?.get("activity") == activityContext.currentActivity) 1f else 0f
            factors++
        }
        
        return if (factors > 0) score / factors else 0.5f
    }

    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        
        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        
        return if (normA > 0 && normB > 0) {
            dotProduct / (sqrt(normA) * sqrt(normB))
        } else {
            0f
        }
    }

    companion object {
        private const val MAX_MEMORY_AGE = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
} 