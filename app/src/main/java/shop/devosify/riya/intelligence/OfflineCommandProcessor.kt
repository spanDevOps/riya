@Singleton
class OfflineCommandProcessor @Inject constructor(
    private val systemMonitorService: SystemMonitorService,
    private val smartHomeService: SmartHomeService,
    private val memoryDao: MemoryDao,
    private val patternAnalysisService: PatternAnalysisService,
    private val ttsService: TtsService,
    private val analyticsService: AnalyticsService
) {
    // Common command patterns
    private val commandPatterns = mapOf(
        "turn (on|off) (?<device>.+)" to CommandType.DEVICE_CONTROL,
        "what('s| is) the (?<query>time|date|weather|battery)" to CommandType.STATUS_QUERY,
        "(set|adjust) (?<setting>volume|brightness) (to )?(?<value>\\d+)" to CommandType.SETTING_ADJUSTMENT,
        "remind me to (?<task>.+) (at|in) (?<time>.+)" to CommandType.REMINDER,
        "where (is|are) (my )?(?<item>.+)" to CommandType.LOCATION_QUERY
    )

    suspend fun processCommand(input: String): OfflineResponse {
        try {
            // Try to match command patterns
            for ((pattern, type) in commandPatterns) {
                val regex = Regex(pattern, RegexOption.IGNORE_CASE)
                val match = regex.find(input)
                
                if (match != null) {
                    return when (type) {
                        CommandType.DEVICE_CONTROL -> handleDeviceControl(match)
                        CommandType.STATUS_QUERY -> handleStatusQuery(match)
                        CommandType.SETTING_ADJUSTMENT -> handleSettingAdjustment(match)
                        CommandType.REMINDER -> handleReminder(match)
                        CommandType.LOCATION_QUERY -> handleLocationQuery(match)
                    }
                }
            }

            // Check for similar past commands in memory
            val similarCommands = findSimilarCommands(input)
            if (similarCommands.isNotEmpty()) {
                val bestMatch = similarCommands.maxByOrNull { it.confidence }!!
                return processSimilarCommand(input, bestMatch)
            }

            // If no direct match, try pattern analysis
            val patterns = patternAnalysisService.findRelevantPatterns(input)
            if (patterns.isNotEmpty()) {
                return handlePatternBasedCommand(patterns.first())
            }

            return OfflineResponse.Unknown("I'm not sure how to help with that while offline")
        } catch (e: Exception) {
            analyticsService.logError("offline_command_processing", e.message ?: "Unknown error")
            return OfflineResponse.Error("Sorry, I encountered an error processing your request")
        }
    }

    private suspend fun handleDeviceControl(match: MatchResult): OfflineResponse {
        val device = match.groups["device"]?.value ?: return OfflineResponse.Error("No device specified")
        val isOn = match.value.contains("on", ignoreCase = true)
        
        return try {
            smartHomeService.controlDevice(device, DeviceCommand.Power(isOn))
            OfflineResponse.Success(
                message = "Turned ${if (isOn) "on" else "off"} $device",
                action = OfflineAction.DeviceControl(device, isOn)
            )
        } catch (e: Exception) {
            OfflineResponse.Error("Failed to control $device")
        }
    }

    private suspend fun handleStatusQuery(match: MatchResult): OfflineResponse {
        return when (match.groups["query"]?.value) {
            "time" -> {
                val time = LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a"))
                OfflineResponse.Success("It's $time")
            }
            "date" -> {
                val date = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
                OfflineResponse.Success("It's $date")
            }
            "battery" -> {
                val batteryStatus = systemMonitorService.getBatteryStatus()
                OfflineResponse.Success(
                    "Battery is at ${batteryStatus.level}% and ${
                        if (batteryStatus.isCharging) "charging" 
                        else "not charging"
                    }"
                )
            }
            else -> OfflineResponse.Error("I can't check that while offline")
        }
    }

    private suspend fun handleSettingAdjustment(match: MatchResult): OfflineResponse {
        val setting = match.groups["setting"]?.value
        val value = match.groups["value"]?.value?.toIntOrNull()
        
        if (setting == null || value == null) {
            return OfflineResponse.Error("Invalid setting or value")
        }

        return when (setting.lowercase()) {
            "volume" -> {
                systemMonitorService.setVolume(value)
                OfflineResponse.Success(
                    message = "Volume set to $value",
                    action = OfflineAction.SettingAdjustment("volume", value)
                )
            }
            "brightness" -> {
                systemMonitorService.setBrightness(value)
                OfflineResponse.Success(
                    message = "Brightness set to $value",
                    action = OfflineAction.SettingAdjustment("brightness", value)
                )
            }
            else -> OfflineResponse.Error("Can't adjust that setting")
        }
    }

    private suspend fun findSimilarCommands(input: String): List<CommandMatch> {
        val recentCommands = memoryDao.getMemoriesByType(MemoryType.COMMAND)
        return recentCommands
            .map { memory ->
                val similarity = calculateSimilarity(input, memory.content)
                CommandMatch(memory, similarity)
            }
            .filter { it.confidence > SIMILARITY_THRESHOLD }
    }

    private fun calculateSimilarity(a: String, b: String): Float {
        // Simple Levenshtein distance-based similarity
        val distance = levenshteinDistance(a.lowercase(), b.lowercase())
        val maxLength = maxOf(a.length, b.length)
        return 1 - (distance.toFloat() / maxLength)
    }

    companion object {
        private const val SIMILARITY_THRESHOLD = 0.8f
    }
}

sealed class OfflineResponse {
    data class Success(
        val message: String,
        val action: OfflineAction? = null
    ) : OfflineResponse()
    
    data class Error(val message: String) : OfflineResponse()
    data class Unknown(val message: String) : OfflineResponse()
}

sealed class OfflineAction {
    data class DeviceControl(
        val device: String,
        val turnOn: Boolean
    ) : OfflineAction()
    
    data class SettingAdjustment(
        val setting: String,
        val value: Int
    ) : OfflineAction()
    
    data class Reminder(
        val task: String,
        val time: LocalDateTime
    ) : OfflineAction()
}

enum class CommandType {
    DEVICE_CONTROL,
    STATUS_QUERY,
    SETTING_ADJUSTMENT,
    REMINDER,
    LOCATION_QUERY
}

data class CommandMatch(
    val memory: MemoryEntity,
    val confidence: Float
) 