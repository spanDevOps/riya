package shop.devosify.riya.service

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalTime
import java.time.DayOfWeek

@Singleton
class VoiceCommandProcessor @Inject constructor(
    private val gptRepository: GptRepository,
    private val taskManager: TaskManager,
    private val memoryRetrievalService: MemoryRetrievalService,
    private val contextManager: ContextManager,
    private val systemMonitorService: SystemMonitorService,
    private val ttsService: TtsService,
    private val analyticsService: AnalyticsService,
    private val communicationService: CommunicationService
) {
    suspend fun processCommand(input: String): Flow<CommandResult> = flow {
        emit(CommandResult.Processing)

        // Build rich context for understanding user intent
        val context = buildContext(input)
        
        // Use GPT to understand the user's intent
        val intentAnalysisPrompt = """
            Context: ${context.toDetailedString()}
            User Input: "$input"
            
            Analyze the user's intention considering:
            1. Previous interactions and context
            2. User's preferences and habits
            3. Current system state
            4. Time of day and location
            5. Recent activities
            
            Return JSON with:
            {
                "intent": {
                    "type": "DEVICE_CONTROL|QUERY|AUTOMATION|MEMORY|SYSTEM|CUSTOM",
                    "confidence": 0.0-1.0,
                    "parameters": {
                        // Extracted parameters based on intent
                    },
                    "clarificationNeeded": boolean,
                    "clarificationQuestion": string or null,
                    "suggestedActions": [string],
                    "relatedIntents": [string]
                }
            }
        """.trimIndent()

        val intentAnalysis = gptRepository.generateText(intentAnalysisPrompt)
            .map { gson.fromJson(it, IntentAnalysis::class.java) }
            .getOrNull()

        if (intentAnalysis == null) {
            emit(CommandResult.Error("Sorry, I couldn't understand your intention"))
            return@flow
        }

        // If clarification is needed, ask user
        if (intentAnalysis.intent.clarificationNeeded) {
            emit(CommandResult.NeedsClarification(
                question = intentAnalysis.intent.clarificationQuestion!!,
                suggestedActions = intentAnalysis.intent.suggestedActions
            ))
            return@flow
        }

        // Process based on understood intent
        val result = when (intentAnalysis.intent.type) {
            IntentType.DEVICE_CONTROL -> {
                handleDeviceControl(intentAnalysis.intent.parameters)
            }
            IntentType.AUTOMATION -> {
                handleAutomation(intentAnalysis.intent.parameters)
            }
            IntentType.QUERY -> {
                handleQuery(intentAnalysis.intent.parameters)
            }
            IntentType.MEMORY -> {
                handleMemory(intentAnalysis.intent.parameters)
            }
            IntentType.SYSTEM -> {
                handleSystemCommand(intentAnalysis.intent.parameters)
            }
            IntentType.CUSTOM -> {
                handleCustomTask(intentAnalysis.intent.parameters)
            }
            IntentType.COMMUNICATION -> {
                handleCommunicationCommand(intentAnalysis.intent.parameters)
            }
        }

        // Store interaction in memory for future context
        memoryRetrievalService.storeInteraction(
            input = input,
            intent = intentAnalysis.intent,
            result = result
        )

        emit(CommandResult.Success(result))
    }

    private suspend fun buildContext(input: String): CommandContext {
        return CommandContext(
            recentMemories = memoryRetrievalService.getRecentMemories(limit = 5),
            currentContext = contextManager.getCurrentContext(),
            systemState = systemMonitorService.getCurrentState(),
            timeContext = TimeContext(
                timeOfDay = LocalTime.now(),
                dayOfWeek = LocalDate.now().dayOfWeek,
                isWeekend = LocalDate.now().dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)
            ),
            userPreferences = memoryRetrievalService.getUserPreferences(),
            recentPatterns = contextManager.getRecentPatterns()
        )
    }

    private suspend fun handleCustomTask(parameters: Map<String, Any>): String {
        // Handle both explicit and implicit task triggers
        val taskIntent = parameters["taskIntent"] as String
        val similarTasks = taskManager.findSimilarTasks(taskIntent)

        return when {
            similarTasks.isEmpty() -> {
                "I don't have any tasks that match your request. Would you like to create one?"
            }
            similarTasks.size == 1 -> {
                taskManager.executeTask(similarTasks.first().id)
                "Executing task: ${similarTasks.first().name}"
            }
            else -> {
                val taskList = similarTasks.joinToString("\n") { "- ${it.name}" }
                "I found several matching tasks:\n$taskList\nWhich one would you like me to execute?"
            }
        }
    }

    private suspend fun handleCommunicationCommand(parameters: Map<String, Any>): String {
        return when (parameters["action"] as String) {
            "send_whatsapp" -> {
                val contact = parameters["contact"] as String
                val message = parameters["message"] as String
                
                communicationService.sendWhatsAppMessage(contact, message)
                    .fold(
                        onSuccess = { "Message sent to $contact" },
                        onFailure = { "Failed to send message: ${it.message}" }
                    )
            }
            "make_call" -> {
                val contact = parameters["contact"] as String
                
                communicationService.makePhoneCall(contact)
                    .fold(
                        onSuccess = { "Calling $contact" },
                        onFailure = { "Failed to make call: ${it.message}" }
                    )
            }
            else -> "Unsupported communication action"
        }
    }
}

data class IntentAnalysis(
    val intent: Intent
)

data class Intent(
    val type: IntentType,
    val confidence: Float,
    val parameters: Map<String, Any>,
    val clarificationNeeded: Boolean,
    val clarificationQuestion: String?,
    val suggestedActions: List<String>,
    val relatedIntents: List<String>
)

enum class IntentType {
    DEVICE_CONTROL,
    QUERY,
    AUTOMATION,
    MEMORY,
    SYSTEM,
    CUSTOM,
    COMMUNICATION
}

sealed class CommandResult {
    object Processing : CommandResult()
    data class Success(val response: String) : CommandResult()
    data class Error(val message: String) : CommandResult()
    data class NeedsClarification(
        val question: String,
        val suggestedActions: List<String>
    ) : CommandResult()
} 