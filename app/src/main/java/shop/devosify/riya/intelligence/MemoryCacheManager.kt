@Singleton
class MemoryCacheManager @Inject constructor(
    private val memoryDao: MemoryDao,
    private val systemMonitorService: SystemMonitorService,
    private val analyticsService: AnalyticsService
) {
    private val memoryCache = LRUCache<String, CachedMemory>(1000)
    private val embeddingCache = LRUCache<String, FloatArray>(2000)
    
    init {
        // Monitor system resources
        viewModelScope.launch {
            systemMonitorService.getSystemState().collect { state ->
                if (state.availableMemory < LOW_MEMORY_THRESHOLD) {
                    trimCaches()
                }
            }
        }
    }

    suspend fun getCachedMemory(key: String): CachedMemory? {
        return memoryCache.get(key)?.let { cached ->
            if (cached.isExpired()) {
                memoryCache.remove(key)
                null
            } else {
                cached
            }
        }
    }

    private fun trimCaches() {
        // Remove old entries
        memoryCache.entries
            .filter { it.value.isExpired() }
            .forEach { memoryCache.remove(it.key) }
            
        // Trim embedding cache if needed
        if (embeddingCache.size > MAX_EMBEDDING_CACHE_SIZE) {
            val toRemove = embeddingCache.size - MAX_EMBEDDING_CACHE_SIZE
            repeat(toRemove) {
                embeddingCache.entries.firstOrNull()?.let {
                    embeddingCache.remove(it.key)
                }
            }
        }
    }

    companion object {
        private const val LOW_MEMORY_THRESHOLD = 100 * 1024 * 1024L // 100MB
        private const val MAX_EMBEDDING_CACHE_SIZE = 1000
    }
} 