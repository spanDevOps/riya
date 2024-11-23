@Singleton
class CommandParser @Inject constructor(
    private val smartHomeService: SmartHomeService,
    private val systemMonitorService: SystemMonitorService,
    private val calendarService: CalendarService,
    private val emailService: EmailService,
    private val locationService: LocationService
) {
    private val commandPatterns = mapOf(
        // Device Control
        "turn (on|off) (.+)" to CommandType.DEVICE_CONTROL,
        "set (brightness|volume) (to )?(.+)" to CommandType.DEVICE_SETTING,
        
        // System Queries
        "what('s| is) (the )?(battery|time|weather|wifi|bluetooth)" to CommandType.SYSTEM_QUERY,
        "check (battery|wifi|bluetooth)" to CommandType.SYSTEM_QUERY,
        
        // Calendar & Tasks
        "schedule (a )?meeting" to CommandType.CALENDAR,
        "what('s| is) (my )?(schedule|agenda|calendar)" to CommandType.CALENDAR,
        "remind me to (.+)" to CommandType.REMINDER,
        
        // Location & Navigation
        "where (am i|is .+)" to CommandType.LOCATION,
        "navigate to (.+)" to CommandType.NAVIGATION,
        
        // Memory & Learning
        "remember (this|that) (.+)" to CommandType.MEMORY,
        "what do you (know|remember) about (.+)" to CommandType.MEMORY_QUERY,
        
        // Settings & Preferences
        "set (.+) preference to (.+)" to CommandType.PREFERENCE,
        "enable|disable (.+)" to CommandType.SETTING
    )

    suspend fun parseCommand(input: String): ParsedCommand {
        for ((pattern, type) in commandPatterns) {
            val regex = pattern.toRegex(RegexOption.IGNORE_CASE)
            val match = regex.find(input)
            
            if (match != null) {
                return when (type) {
                    CommandType.DEVICE_CONTROL -> parseDeviceControl(match)
                    CommandType.SYSTEM_QUERY -> parseSystemQuery(match)
                    CommandType.CALENDAR -> parseCalendarCommand(match)
                    CommandType.REMINDER -> parseReminder(match)
                    CommandType.LOCATION -> parseLocationCommand(match)
                    CommandType.PREFERENCE -> parsePreference(match)
                    else -> ParsedCommand.Unknown(input)
                }
            }
        }
        return ParsedCommand.Unknown(input)
    }

    private suspend fun parseDeviceControl(match: MatchResult): ParsedCommand {
        val action = match.groupValues[1] // "on" or "off"
        val device = match.groupValues[2].trim()
        
        return ParsedCommand.DeviceControl(
            device = device,
            action = if (action == "on") DeviceAction.ON else DeviceAction.OFF
        )
    }

    private suspend fun parseSystemQuery(match: MatchResult): ParsedCommand {
        val queryType = match.groupValues.last().trim()
        return ParsedCommand.SystemQuery(
            type = when (queryType) {
                "battery" -> SystemQueryType.BATTERY
                "wifi" -> SystemQueryType.WIFI
                "bluetooth" -> SystemQueryType.BLUETOOTH
                "time" -> SystemQueryType.TIME
                else -> SystemQueryType.UNKNOWN
            }
        )
    }
}

sealed class ParsedCommand {
    data class DeviceControl(
        val device: String,
        val action: DeviceAction
    ) : ParsedCommand()
    
    data class SystemQuery(
        val type: SystemQueryType
    ) : ParsedCommand()
    
    data class Calendar(
        val action: CalendarAction,
        val details: Map<String, Any>
    ) : ParsedCommand()
    
    data class Reminder(
        val task: String,
        val time: Long? = null
    ) : ParsedCommand()
    
    data class Unknown(val originalInput: String) : ParsedCommand()
}

enum class DeviceAction { ON, OFF, ADJUST }
enum class SystemQueryType { BATTERY, WIFI, BLUETOOTH, TIME, UNKNOWN }
enum class CalendarAction { VIEW, CREATE, UPDATE, DELETE } 