package shop.devosify.riya.service

import javax.inject.Inject
import javax.inject.Singleton
import shop.devosify.riya.repository.GptRepository
import shop.devosify.riya.data.local.entity.MemoryEntity
import shop.devosify.riya.data.local.dao.MemoryDao

@Singleton
class MemoryExtractionService @Inject constructor(
    private val gptRepository: GptRepository,
    private val memoryDao: MemoryDao
) {
    suspend fun extractMemories(conversation: String): Result<List<MemoryEntity>> {
        val prompt = """
            Analyze this conversation and extract key information:
            $conversation
            
            Return format: JSON array of memories with:
            - type: FACT, PREFERENCE, HABIT, GOAL
            - content: the actual information
            - importance: 1-5 (5 being most important)
            - context: relevant contextual information
        """.trimIndent()

        return gptRepository.generateText(prompt).map { response ->
            parseMemories(response)
        }
    }
} 