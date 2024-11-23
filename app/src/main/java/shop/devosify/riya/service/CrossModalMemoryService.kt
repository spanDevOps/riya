@Singleton
class CrossModalMemoryService @Inject constructor(
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository,
    private val cameraVisionService: CameraVisionService
) {
    suspend fun createVisualMemory(image: Bitmap): VisualMemory {
        val analysis = cameraVisionService.analyzeImage(image)
        return VisualMemory(
            description = analysis.description,
            objects = analysis.detectedObjects,
            emotions = analysis.detectedEmotions,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun linkMemories(
        visualMemory: VisualMemory,
        conversationMemory: ConversationMemory
    ): LinkedMemory {
        // Link visual and conversation memories
        return LinkedMemory(
            visualMemoryId = visualMemory.id,
            conversationMemoryId = conversationMemory.id,
            relationship = analyzeRelationship(visualMemory, conversationMemory)
        )
    }
}

data class LinkedMemory(
    val visualMemoryId: Long,
    val conversationMemoryId: Long,
    val relationshipType: RelationType,
    val confidence: Float,
    val description: String
)

enum class RelationType {
    CAUSAL,
    TEMPORAL,
    SPATIAL,
    SEMANTIC
} 