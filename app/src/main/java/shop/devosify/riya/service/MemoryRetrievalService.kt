package shop.devosify.riya.service

import javax.inject.Inject
import javax.inject.Singleton
import shop.devosify.riya.data.local.dao.MemoryDao
import shop.devosify.riya.data.local.entity.MemoryEntity

@Singleton
class MemoryRetrievalService @Inject constructor(
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository
) {
    private val contextPrompt = """
        Based on these memories about the user, generate a concise context summary.
        Focus on the most important and relevant information.
        Format: A brief paragraph that can be used as context for the next conversation.
    """.trimIndent()

    suspend fun getRelevantContext(input: String): String {
        // Search for relevant memories based on input
        val relevantMemories = memoryDao.searchMemories(input)
            .sortedByDescending { it.importance }
            .take(5) // Take top 5 most important relevant memories

        if (relevantMemories.isEmpty()) {
            return ""
        }

        // Format memories for GPT
        val memoriesText = relevantMemories.joinToString("\n") { memory ->
            "- ${memory.content} (from ${memory.context}, importance: ${memory.importance})"
        }

        // Generate context summary
        return gptRepository.generateText("$contextPrompt\n\nMemories:\n$memoriesText")
            .getOrDefault("")
    }

    suspend fun getImportantMemories(category: String? = null): List<MemoryEntity> {
        return if (category != null) {
            memoryDao.getMemoriesByCategory(category)
                .firstOrNull()
                ?.filter { it.importance >= 4 }
                ?: emptyList()
        } else {
            memoryDao.searchMemories("")
                .filter { it.importance >= 4 }
        }
    }
} 