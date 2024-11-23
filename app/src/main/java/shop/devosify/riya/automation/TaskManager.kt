@Singleton
class TaskManager @Inject constructor(
    private val taskConfigurationService: TaskConfigurationService,
    private val taskExecutionService: TaskExecutionService,
    private val gptRepository: GptRepository,
    private val analyticsService: AnalyticsService,
    private val memoryDao: MemoryDao
) {
    private val _tasks = MutableStateFlow<List<CustomTask>>(emptyList())
    val tasks: StateFlow<List<CustomTask>> = _tasks.asStateFlow()

    init {
        loadTasks()
    }

    private fun loadTasks() {
        viewModelScope.launch {
            _tasks.value = taskConfigurationService.getStoredTasks()
        }
    }

    suspend fun configureNewTask(userDescription: String): Result<CustomTask> {
        return try {
            // First, analyze the task description for feasibility
            val analysisPrompt = """
                Analyze this task description for feasibility:
                "$userDescription"
                
                Consider:
                1. Required permissions
                2. Technical limitations
                3. Security implications
                4. Resource requirements
                
                Return JSON with:
                {
                    "feasible": boolean,
                    "concerns": [string],
                    "suggestedModifications": [string],
                    "estimatedComplexity": "LOW|MEDIUM|HIGH"
                }
            """.trimIndent()

            val analysis = gptRepository.generateText(analysisPrompt)
                .getOrThrow()
                .let { gson.fromJson(it, TaskAnalysis::class.java) }

            if (!analysis.feasible) {
                return Result.failure(
                    IllegalArgumentException(
                        "Task not feasible: ${analysis.concerns.joinToString()}"
                    )
                )
            }

            // Configure the task
            val task = taskConfigurationService.configureNewTask(userDescription)
                .getOrThrow()

            // Store task
            _tasks.update { current -> current + task }

            // Create memory of task creation
            memoryDao.insertMemory(MemoryEntity(
                type = MemoryType.AUTOMATION,
                content = "Created automation task: ${task.name}",
                importance = 4,
                tags = listOf("automation", "task_creation"),
                metadata = mapOf(
                    "task_id" to task.id,
                    "trigger" to task.trigger,
                    "complexity" to analysis.estimatedComplexity
                )
            ))

            analyticsService.logEvent("task_configured", mapOf(
                "task_id" to task.id,
                "complexity" to analysis.estimatedComplexity,
                "steps_count" to task.steps.size
            ))

            Result.success(task)
        } catch (e: Exception) {
            analyticsService.logError("task_configuration", e.message ?: "Unknown error")
            Result.failure(e)
        }
    }

    suspend fun executeTask(taskId: String): Result<Unit> {
        val task = _tasks.value.find { it.id == taskId }
            ?: return Result.failure(IllegalArgumentException("Task not found"))

        return taskExecutionService.executeTask(task)
            .onSuccess {
                analyticsService.logEvent("task_executed", mapOf(
                    "task_id" to task.id,
                    "success" to true
                ))
            }
            .onFailure { error ->
                analyticsService.logError("task_execution", error.message ?: "Unknown error")
            }
    }

    suspend fun deleteTask(taskId: String) {
        _tasks.update { current -> current.filterNot { it.id == taskId } }
        taskConfigurationService.deleteTask(taskId)
    }

    suspend fun updateTask(task: CustomTask): Result<CustomTask> {
        return taskConfigurationService.updateTask(task)
            .also { result ->
                result.onSuccess { updatedTask ->
                    _tasks.update { current ->
                        current.map { if (it.id == updatedTask.id) updatedTask else it }
                    }
                }
            }
    }
}

data class TaskAnalysis(
    val feasible: Boolean,
    val concerns: List<String>,
    val suggestedModifications: List<String>,
    val estimatedComplexity: String
) 