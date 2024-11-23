@Singleton
class UserActivityMonitor @Inject constructor(
    private val contentResolver: ContentResolver,
    private val gptRepository: GptRepository,
    private val memoryDao: MemoryDao,
    private val analyticsService: AnalyticsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        scope.launch {
            monitorUserActivities()
        }
    }

    private suspend fun monitorUserActivities() {
        combine(
            monitorBrowserHistory(),
            monitorYouTubeHistory(),
            monitorInstalledApps(),
            monitorAppUsage()
        ) { browserHistory, youtubeHistory, installedApps, appUsage ->
            analyzeUserInterests(
                browserHistory = browserHistory,
                youtubeHistory = youtubeHistory,
                installedApps = installedApps,
                appUsage = appUsage
            )
        }.collect { interests ->
            updateUserProfile(interests)
        }
    }

    private suspend fun analyzeUserInterests(
        browserHistory: List<BrowserHistory>,
        youtubeHistory: List<YoutubeHistory>,
        installedApps: List<AppInfo>,
        appUsage: List<AppUsage>
    ): List<UserInterest> {
        val prompt = """
            Analyze user's recent activities:
            Browser History: ${browserHistory.joinToString("\n")}
            YouTube History: ${youtubeHistory.joinToString("\n")}
            Installed Apps: ${installedApps.joinToString("\n")}
            App Usage: ${appUsage.joinToString("\n")}
            
            Identify:
            1. Main interests and topics
            2. Learning patterns
            3. Entertainment preferences
            4. Productivity tools used
            5. Potential needs or interests
            
            Return: JSON array of interests with confidence scores
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<UserInterest>::class.java).toList()
            }
            .getOrDefault(emptyList())
    }

    private suspend fun updateUserProfile(interests: List<UserInterest>) {
        // Store in memory system
        interests.forEach { interest ->
            memoryDao.insertMemory(MemoryEntity(
                type = MemoryType.USER_INTEREST,
                content = interest.description,
                importance = (interest.confidence * 5).toInt(),
                tags = interest.categories,
                timestamp = System.currentTimeMillis()
            ))
        }

        // Generate proactive suggestions
        generateSuggestions(interests)
    }

    private suspend fun generateSuggestions(interests: List<UserInterest>) {
        val prompt = """
            Based on user interests:
            ${interests.joinToString("\n")}
            
            Suggest:
            1. Relevant learning resources
            2. Related content
            3. Useful tools or apps
            4. Potential activities
            
            Consider user's current context and preferences.
            Return: JSON array of actionable suggestions
        """.trimIndent()

        gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<Suggestion>::class.java).toList()
            }
            .onSuccess { suggestions ->
                suggestions.forEach { suggestion ->
                    notifySuggestion(suggestion)
                }
            }
    }
}

data class UserInterest(
    val topic: String,
    val description: String,
    val confidence: Float,
    val categories: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

data class Suggestion(
    val type: SuggestionType,
    val title: String,
    val description: String,
    val action: String?,
    val priority: Int
)

enum class SuggestionType {
    LEARNING_RESOURCE,
    CONTENT,
    TOOL,
    ACTIVITY
} 