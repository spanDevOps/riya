@Singleton
class TaskConfigurationService @Inject constructor(
    private val gptRepository: GptRepository,
    private val memoryDao: MemoryDao,
    private val secureStorage: SecureStorage,
    private val analyticsService: AnalyticsService
) {
    private val gson = Gson()

    suspend fun configureNewTask(userDescription: String): Result<CustomTask> {
        // Use GPT to parse the user's description into structured steps
        val prompt = """
            Parse this task description into structured steps:
            "$userDescription"
            
            Convert into a precise sequence of actions.
            Return JSON with:
            {
                "trigger": "voice command that activates this task",
                "name": "short name for this task",
                "description": "user-friendly description",
                "steps": [
                    {
                        "type": "CLICK/SEARCH/INPUT/WAIT/LAUNCH",
                        "target": "element identifier or app name",
                        "value": "text to enter or button to click",
                        "delay": "wait time in ms if needed"
                    }
                ],
                "required_permissions": ["list", "of", "permissions"],
                "platforms": ["websites or apps needed"]
            }
        """.trimIndent()

        return gptRepository.generateText(prompt).map { response ->
            val taskConfig = gson.fromJson(response, CustomTask::class.java)
            
            // Store the task configuration
            storeCustomTask(taskConfig)
            
            // Create a memory for this task
            memoryDao.insertMemory(MemoryEntity(
                type = MemoryType.AUTOMATION,
                content = "Custom task: ${taskConfig.name}\nTrigger: ${taskConfig.trigger}",
                importance = 4,
                tags = listOf("automation", "custom_task"),
                metadata = mapOf(
                    "task_id" to taskConfig.id,
                    "platforms" to taskConfig.platforms
                )
            ))

            taskConfig
        }
    }

    private suspend fun storeCustomTask(task: CustomTask) {
        val tasks = getStoredTasks().toMutableList()
        tasks.add(task)
        secureStorage.storeSensitiveData(
            CUSTOM_TASKS_KEY,
            gson.toJson(tasks).toByteArray()
        )
    }

    suspend fun getStoredTasks(): List<CustomTask> {
        return secureStorage.retrieveSensitiveData(CUSTOM_TASKS_KEY)?.let { data ->
            gson.fromJson(
                data.toString(Charsets.UTF_8),
                Array<CustomTask>::class.java
            ).toList()
        } ?: emptyList()
    }

    companion object {
        private const val CUSTOM_TASKS_KEY = "custom_tasks"
    }
}

data class CustomTask(
    val id: String = UUID.randomUUID().toString(),
    val trigger: String,
    val name: String,
    val description: String,
    val steps: List<TaskStep>,
    val requiredPermissions: List<String>,
    val platforms: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)

data class TaskStep(
    val type: TaskStepType,
    val target: String,
    val value: String? = null,
    val delay: Long? = null
)

enum class TaskStepType {
    CLICK, SEARCH, INPUT, WAIT, LAUNCH
} 