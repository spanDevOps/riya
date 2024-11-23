@Singleton
class PersonalGrowthService @Inject constructor(
    private val gptRepository: GptRepository,
    private val userActivityMonitor: UserActivityMonitor,
    private val memoryDao: MemoryDao
) {
    suspend fun analyzePersonalGrowth() {
        val activities = userActivityMonitor.getRecentActivities()
        val learningPatterns = analyzePatterns(activities)
        suggestGrowthOpportunities(learningPatterns)
    }

    suspend fun generateInsights() {
        val memories = memoryDao.getRecentMemories()
        val patterns = findBehavioralPatterns(memories)
        providePersonalInsights(patterns)
    }
} 