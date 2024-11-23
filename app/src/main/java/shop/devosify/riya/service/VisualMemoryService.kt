@Singleton
class VisualMemoryService @Inject constructor(
    private val cameraVisionService: CameraVisionService,
    private val gptRepository: GptRepository,
    private val memoryDao: MemoryDao
) {
    suspend fun processVisualMemory(image: Bitmap): Result<VisualMemory> {
        return try {
            val analysis = cameraVisionService.analyzeImage(image)
            val prompt = """
                Analyze this scene and extract key information:
                Scene: ${analysis.description}
                Objects: ${analysis.detectedObjects.joinToString()}
                
                Return format: JSON with:
                - importance: 1-5 (5 being most important)
                - context: relevant contextual information
                - relationships: potential connections to previous memories
            """.trimIndent()

            gptRepository.generateText(prompt).map { response ->
                val metadata = parseMetadata(response)
                VisualMemory(
                    description = analysis.description,
                    objects = analysis.detectedObjects,
                    emotions = analysis.detectedEmotions,
                    importance = metadata.importance,
                    context = metadata.context,
                    timestamp = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseMetadata(response: String): VisualMetadata {
        return gson.fromJson(response, VisualMetadata::class.java)
    }
}

data class VisualMetadata(
    val importance: Int,
    val context: String,
    val relationships: List<String>
) 