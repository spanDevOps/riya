@Singleton
class EmotionalContextService @Inject constructor(
    private val emotionRecognitionService: EmotionRecognitionService,
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository
) {
    private val emotionHistory = mutableListOf<EmotionState>()
    private val emotionalProfile = MutableStateFlow<EmotionalProfile?>(null)

    suspend fun updateEmotionalContext(emotion: EmotionState) {
        emotionHistory.add(emotion)
        if (emotionHistory.size > MAX_HISTORY_SIZE) {
            emotionHistory.removeAt(0)
        }

        // Analyze emotional patterns
        val prompt = """
            Analyze this emotional history and provide insights:
            History: ${emotionHistory.joinToString { "${it.emotion} (${it.confidence})" }}
            
            Return format: JSON with:
            - dominantMood: overall emotional state
            - stability: 1-5 (5 being very stable)
            - suggestions: list of interaction adjustments
        """.trimIndent()

        gptRepository.generateText(prompt).onSuccess { response ->
            val profile = gson.fromJson(response, EmotionalProfile::class.java)
            emotionalProfile.value = profile
        }
    }

    fun getEmotionalProfile(): EmotionalProfile? = emotionalProfile.value

    companion object {
        private const val MAX_HISTORY_SIZE = 10
    }
}

data class EmotionalProfile(
    val dominantMood: String,
    val stability: Int,
    val suggestions: List<String>
) 