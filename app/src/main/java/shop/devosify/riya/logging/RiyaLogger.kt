@Singleton
class RiyaLogger @Inject constructor(
    private val analyticsService: AnalyticsService
) {
    private val logBuffer = CircularBuffer<LogEntry>(1000)

    fun log(
        level: LogLevel,
        tag: String,
        message: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        
        logBuffer.add(entry)
        
        if (level >= LogLevel.ERROR) {
            analyticsService.logEvent("error_logged", mapOf(
                "level" to level.name,
                "tag" to tag,
                "message" to message
            ))
        }
    }

    fun getLogs(
        level: LogLevel? = null,
        tag: String? = null,
        since: Long? = null
    ): List<LogEntry> {
        return logBuffer.toList()
            .filter { entry ->
                (level == null || entry.level >= level) &&
                (tag == null || entry.tag == tag) &&
                (since == null || entry.timestamp >= since)
            }
    }
}

enum class LogLevel {
    DEBUG, INFO, WARNING, ERROR, CRITICAL
}

data class LogEntry(
    val level: LogLevel,
    val tag: String,
    val message: String,
    val metadata: Map<String, Any>,
    val timestamp: Long
) 