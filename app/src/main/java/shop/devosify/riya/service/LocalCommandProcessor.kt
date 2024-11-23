@Singleton
class LocalCommandProcessor @Inject constructor(
    private val memoryDao: MemoryDao,
    private val systemMonitorService: SystemMonitorService,
    private val smartHomeService: SmartHomeService,
    private val patternAnalysisService: PatternAnalysisService,
    private val analyticsService: AnalyticsService
) {
    // Natural language patterns for command matching
    private val commandPatterns = mapOf(
        // Device control patterns
        "turn (on|off) (?<device>.*)" to CommandType.DEVICE_CONTROL,
        "(increase|decrease|set) (?<property>volume|brightness) (?<value>\\d+)?" to CommandType.PROPERTY_CONTROL,
        
        // System queries
        "how('s| is) (the )?(?<query>battery|wifi|bluetooth|storage)" to CommandType.SYSTEM_QUERY,
        "what('s| is) (the )?(?<query>time|date|weather|temperature)" to CommandType.INFO_QUERY,
        
        // Smart home queries
        "(which|what|list) devices (are )?(turned )?(on|off|connected)" to CommandType.DEVICE_QUERY,
        "is (?<device>.*) (turned )?(on|off)" to CommandType.DEVICE_STATUS,
        
        // Context-aware patterns
        "do (the )?same( thing)? (as|like) (?<context>last time|yesterday|before)" to CommandType.CONTEXTUAL,
        "(repeat|do it again|one more time)" to CommandType.REPEAT_LAST
    )

    private var lastCommand: ProcessedCommand? = null
    private val commandHistory = mutableListOf<ProcessedCommand>()

    suspend fun processLocalCommand(input: String): LocalCommandResult {
        try {
            // 1. Pattern Matching
            val (type, matches) = findMatchingPattern(input) ?: return LocalCommandResult.Unrecognized

            // 2. Context Enhancement
            val context = enrichCommandContext(input, type, matches)

            // 3. Command Processing
            val result = when (type) {
                CommandType.DEVICE_CONTROL -> handleDeviceControl(matches, context)
                CommandType.PROPERTY_CONTROL -> handlePropertyControl(matches, context)
                CommandType.SYSTEM_QUERY -> handleSystemQuery(matches)
                CommandType.INFO_QUERY -> handleInfoQuery(matches)
                CommandType.DEVICE_QUERY -> handleDeviceQuery(matches)
                CommandType.DEVICE_STATUS -> handleDeviceStatus(matches)
                CommandType.CONTEXTUAL -> handleContextualCommand(matches)
                CommandType.REPEAT_LAST -> handleRepeatCommand()
            }

            // 4. Learning & Pattern Analysis
            if (result is LocalCommandResult.Success) {
                learnFromCommand(ProcessedCommand(input, type, matches, result.response))
            }

            return result

        } catch (e: Exception) {
            analyticsService.logError("local_command_processing", e.message ?: "Unknown error")
            return LocalCommandResult.Error("Sorry, I couldn't process that command locally")
        }
    }

    private suspend fun enrichCommandContext(
        input: String,
        type: CommandType,
        matches: Map<String, String>
    ): CommandContext {
        // Get relevant patterns from pattern analysis
        val patterns = patternAnalysisService.getUserPatterns().first()
        
        // Get recent related memories
        val recentMemories = memoryDao.getRecentMemoriesByType(type.toString())
        
        // Get current system state
        val systemState = systemMonitorService.getCurrentState()
        
        return CommandContext(
            timeOfDay = LocalTime.now(),
            dayOfWeek = LocalDate.now().dayOfWeek,
            userPatterns = patterns,
            recentMemories = recentMemories,
            systemState = systemState
        )
    }

    private suspend fun handleDeviceControl(
        matches: Map<String, String>, 
        context: CommandContext
    ): LocalCommandResult {
        val deviceName = matches["device"] ?: return LocalCommandResult.Error("No device specified")
        val action = matches["action"] ?: "toggle"

        // Check user patterns for device preferences
        val devicePattern = context.userPatterns.find { 
            it.type == PatternType.PREFERENCE && 
            it.description.contains(deviceName, ignoreCase = true) 
        }

        // Apply learned preferences
        val adjustedAction = devicePattern?.let { pattern ->
            when {
                pattern.timeContext?.timeOfDay?.contains(context.timeOfDay.hour) == true -> {
                    // User typically wants this device on/off at this time
                    if (pattern.description.contains("on", ignoreCase = true)) "on" else "off"
                }
                else -> action
            }
        } ?: action

        return try {
            val result = smartHomeService.controlDevice(
                deviceName,
                DeviceCommand.Power(adjustedAction == "on")
            )
            LocalCommandResult.Success(
                "OK, $deviceName is now $adjustedAction" +
                if (adjustedAction != action) " (adjusted based on your usual preferences)" else ""
            )
        } catch (e: Exception) {
            LocalCommandResult.Error("Couldn't control $deviceName")
        }
    }

    private suspend fun learnFromCommand(command: ProcessedCommand) {
        // Store command in history
        commandHistory.add(command)
        if (commandHistory.size > MAX_HISTORY_SIZE) {
            commandHistory.removeAt(0)
        }
        lastCommand = command

        // Extract patterns
        patternAnalysisService.analyzeAndUpdatePatterns()
    }

    data class CommandContext(
        val timeOfDay: LocalTime,
        val dayOfWeek: DayOfWeek,
        val userPatterns: List<UserPattern>,
        val recentMemories: List<MemoryEntity>,
        val systemState: SystemState
    )

    data class ProcessedCommand(
        val input: String,
        val type: CommandType,
        val matches: Map<String, String>,
        val response: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    sealed class LocalCommandResult {
        data class Success(val response: String) : LocalCommandResult()
        data class Error(val message: String) : LocalCommandResult()
        object Unrecognized : LocalCommandResult()
    }

    companion object {
        private const val MAX_HISTORY_SIZE = 50
    }
} 