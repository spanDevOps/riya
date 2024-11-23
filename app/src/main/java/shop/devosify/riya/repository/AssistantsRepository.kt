package shop.devosify.riya.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shop.devosify.riya.models.ContextRequest
import shop.devosify.riya.service.AssistantsApiService
import shop.devosify.riya.service.MemoryRetrievalService
import shop.devosify.riya.service.SmartHomeService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AssistantsRepository @Inject constructor(
    private val assistantsApiService: AssistantsApiService,
    private val memoryRetrievalService: MemoryRetrievalService,
    private val smartHomeService: SmartHomeService
) {
    suspend fun createAssistant(name: String, instructions: String) = withContext(Dispatchers.IO) {
        try {
            val request = CreateAssistantRequest(
                name = name,
                instructions = instructions
            )
            val response = assistantsApiService.createAssistant(request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create assistant: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createThread() = withContext(Dispatchers.IO) {
        try {
            val response = assistantsApiService.createThread()
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to create thread: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendMessage(threadId: String, content: String) = withContext(Dispatchers.IO) {
        try {
            // Get relevant context from memories
            val context = memoryRetrievalService.getRelevantContext(content)
            
            // Add context to the message if available
            val messageContent = if (context.isNotBlank()) {
                """
                Context: $context
                
                User message: $content
                """.trimIndent()
            } else {
                content
            }

            val request = AddMessageRequest(content = messageContent)
            val response = assistantsApiService.addMessage(threadId, request)
            if (response.isSuccessful) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Failed to send message: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createAndWaitForRun(threadId: String, assistantId: String) = withContext(Dispatchers.IO) {
        try {
            val request = CreateRunRequest(assistant_id = assistantId)
            val runResponse = assistantsApiService.createRun(threadId, request)
            if (!runResponse.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to create run: ${runResponse.code()}"))
            }

            val run = runResponse.body()!!
            var currentRun = run
            while (currentRun.status in listOf("queued", "in_progress")) {
                kotlinx.coroutines.delay(1000) // Poll every second
                val statusResponse = assistantsApiService.retrieveRun(threadId, run.id)
                if (!statusResponse.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to check run status: ${statusResponse.code()}"))
                }
                currentRun = statusResponse.body()!!
            }

            if (currentRun.status == "completed") {
                Result.success(currentRun)
            } else {
                Result.failure(Exception("Run failed with status: ${currentRun.status}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun handleSystemCommand(command: String): String {
        return when {
            command.contains("turn on") || command.contains("turn off") -> {
                val devices = smartHomeService.getConnectedDevices().first()
                val deviceName = extractDeviceName(command)
                val turnOn = command.contains("turn on")
                
                devices.find { it.name.equals(deviceName, ignoreCase = true) }?.let { device ->
                    smartHomeService.controlDevice(device.id, DeviceCommand.Power(turnOn))
                    "Done! ${device.name} is now ${if (turnOn) "on" else "off"}"
                } ?: "Sorry, I couldn't find a device named $deviceName"
            }
            // Add other command patterns
            else -> "I'm not sure how to handle that command"
        }
    }

    private fun extractDeviceName(command: String): String {
        // Use regex or string manipulation to extract device name
        // Example: "turn on living room lights" -> "living room lights"
        return command.substringAfter("turn on ")
            .substringAfter("turn off ")
    }
}
