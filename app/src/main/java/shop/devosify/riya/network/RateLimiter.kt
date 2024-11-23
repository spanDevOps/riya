@Singleton
class RateLimiter @Inject constructor(
    private val analyticsService: RiyaAnalytics
) {
    private val limits = mapOf(
        "gpt" to TokenBucket(3000, 3000), // 3000 requests per minute
        "whisper" to TokenBucket(50, 50),  // 50 requests per minute
        "tts" to TokenBucket(100, 100)     // 100 requests per minute
    )

    private val quotas = mutableMapOf<String, Int>()
    private val retryAfterTimes = mutableMapOf<String, Long>()

    suspend fun checkLimit(service: String): Boolean {
        val bucket = limits[service] ?: return true
        
        // Check if we're in retry-after period
        retryAfterTimes[service]?.let { retryAfter ->
            if (System.currentTimeMillis() < retryAfter) {
                return false
            } else {
                retryAfterTimes.remove(service)
            }
        }

        return bucket.tryConsume()
    }

    fun updateQuota(service: String, remaining: Int, resetAt: Long) {
        quotas[service] = remaining
        if (remaining <= 0) {
            retryAfterTimes[service] = resetAt
            analyticsService.logEvent("rate_limit_reached", mapOf(
                "service" to service,
                "reset_at" to resetAt
            ))
        }
    }

    fun getRemainingQuota(service: String): Int {
        return quotas[service] ?: limits[service]?.capacity ?: Int.MAX_VALUE
    }

    private class TokenBucket(
        val capacity: Int,
        initialTokens: Int,
        private val refillRate: Float = capacity.toFloat() / 60f // Tokens per second
    ) {
        private var tokens = initialTokens.toFloat()
        private var lastRefill = System.currentTimeMillis()

        @Synchronized
        fun tryConsume(): Boolean {
            refill()
            return if (tokens >= 1) {
                tokens--
                true
            } else {
                false
            }
        }

        private fun refill() {
            val now = System.currentTimeMillis()
            val elapsedSeconds = (now - lastRefill) / 1000f
            val refill = elapsedSeconds * refillRate
            
            tokens = (tokens + refill).coerceAtMost(capacity.toFloat())
            lastRefill = now
        }
    }
} 