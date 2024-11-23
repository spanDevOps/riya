@Singleton
class CreativityAssistant @Inject constructor(
    private val gptRepository: GptRepository,
    private val userInterestsManager: UserInterestsManager
) {
    suspend fun generateCreativeIdeas() {
        val interests = userInterestsManager.getUserInterests()
        val ideas = generateIdeasBasedOnInterests(interests)
        presentCreativeIdeas(ideas)
    }

    suspend fun suggestCreativeActivities() {
        val mood = detectUserMood()
        val availableTime = getAvailableTime()
        suggestActivity(mood, availableTime)
    }
} 