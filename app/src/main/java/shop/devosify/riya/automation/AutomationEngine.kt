@Singleton
class AutomationEngine @Inject constructor(
    private val locationService: LocationService,
    private val patternAnalysisService: PatternAnalysisService,
    private val systemMonitorService: SystemMonitorService,
    private val smartHomeService: SmartHomeService,
    private val emotionalContextService: EmotionalContextService,
    private val memoryRetrievalService: MemoryRetrievalService,
    private val ttsService: TtsService,
    private val analyticsService: AnalyticsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _automationState = MutableStateFlow<List<AutomationRule>>(emptyList())
    val automationState: StateFlow<List<AutomationRule>> = _automationState.asStateFlow()

    init {
        startAutomationEngine()
    }

    private fun startAutomationEngine() {
        scope.launch {
            // Combine multiple triggers
            combine(
                locationService.getLocationUpdates(),
                patternAnalysisService.getUserPatterns(),
                systemMonitorService.getSystemState(),
                emotionalContextService.getEmotionalProfile()
            ) { location, patterns, systemState, emotion ->
                checkAndTriggerAutomations(
                    AutomationContext(
                    location = location,
                    patterns = patterns,
                    systemState = systemState,
                    emotion = emotion,
                    time = LocalDateTime.now()
                )
                )
            }.collect()
        }
    }

    suspend fun addAutomationRule(rule: AutomationRule) {
        val currentRules = _automationState.value.toMutableList()
        currentRules.add(rule)
        _automationState.value = currentRules
        analyticsService.logEvent("automation_rule_added", mapOf(
            "type" to rule.trigger.type.name,
            "actions" to rule.actions.map { it.type.name }
        ))
    }

    private suspend fun checkAndTriggerAutomations(context: AutomationContext) {
        _automationState.value.forEach { rule ->
            if (shouldTriggerRule(rule, context)) {
                executeActions(rule.actions, context)
                analyticsService.logEvent("automation_triggered", mapOf(
                    "rule_id" to rule.id,
                    "trigger_type" to rule.trigger.type.name
                ))
            }
        }
    }

    private suspend fun shouldTriggerRule(rule: AutomationRule, context: AutomationContext): Boolean {
        return when (rule.trigger.type) {
            TriggerType.LOCATION -> checkLocationTrigger(rule.trigger, context)
            TriggerType.TIME -> checkTimeTrigger(rule.trigger, context)
            TriggerType.DEVICE_STATE -> checkDeviceStateTrigger(rule.trigger, context)
            TriggerType.PATTERN -> checkPatternTrigger(rule.trigger, context)
            TriggerType.EMOTIONAL -> checkEmotionalTrigger(rule.trigger, context)
        }
    }

    private suspend fun checkLocationTrigger(trigger: Trigger, context: AutomationContext): Boolean {
        val location = trigger.conditions["location"] as LocationTrigger
        return when (location.type) {
            LocationType.ENTER -> context.location.isInside(location.area)
            LocationType.EXIT -> !context.location.isInside(location.area)
            LocationType.DWELL -> context.location.hasDwelledIn(location.area, location.duration)
        }
    }

    private suspend fun checkTimeTrigger(trigger: Trigger, context: AutomationContext): Boolean {
        val timeCondition = trigger.conditions["time"] as TimeCondition
        return when (timeCondition.type) {
            TimeType.SPECIFIC -> context.time.matches(timeCondition.time)
            TimeType.RECURRING -> isRecurringTimeMatch(timeCondition, context.time)
            TimeType.RELATIVE -> checkRelativeTime(timeCondition, context)
        }
    }

    private suspend fun executeActions(actions: List<AutomationAction>, context: AutomationContext) {
        actions.forEach { action ->
            try {
                when (action.type) {
                    ActionType.DEVICE_CONTROL -> executeDeviceAction(action)
                    ActionType.NOTIFICATION -> sendNotification(action)
                    ActionType.SCENE -> activateScene(action)
                    ActionType.VOICE -> speakAction(action)
                    ActionType.SYSTEM -> executeSystemAction(action)
                }
            } catch (e: Exception) {
                analyticsService.logError("automation_action", e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun executeDeviceAction(action: AutomationAction) {
        val deviceId = action.parameters["deviceId"] as Long
        val command = action.parameters["command"] as DeviceCommand
        smartHomeService.controlDevice(deviceId, command)
    }

    private suspend fun speakAction(action: AutomationAction) {
        val message = action.parameters["message"] as String
        val context = action.parameters["context"] as? Map<String, Any>
        
        // Enhance message with context if available
        val enhancedMessage = context?.let { 
            enrichMessageWithContext(message, it)
        } ?: message
        
        ttsService.speak(enhancedMessage)
    }

    private suspend fun enrichMessageWithContext(
        message: String, 
        context: Map<String, Any>
    ): String {
        // Replace placeholders with context values
        var enrichedMessage = message
        context.forEach { (key, value) ->
            enrichedMessage = enrichedMessage.replace("{$key}", value.toString())
        }
        return enrichedMessage
    }
}

data class AutomationRule(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val trigger: Trigger,
    val actions: List<AutomationAction>,
    val conditions: List<Condition> = emptyList(),
    val priority: Int = 0,
    val enabled: Boolean = true
)

data class Trigger(
    val type: TriggerType,
    val conditions: Map<String, Any>
)

enum class TriggerType {
    LOCATION, TIME, DEVICE_STATE, PATTERN, EMOTIONAL
}

data class AutomationAction(
    val type: ActionType,
    val parameters: Map<String, Any>
)

enum class ActionType {
    DEVICE_CONTROL, NOTIFICATION, SCENE, VOICE, SYSTEM
}

data class AutomationContext(
    val location: Location,
    val patterns: List<UserPattern>,
    val systemState: SystemState,
    val emotion: EmotionalProfile,
    val time: LocalDateTime
) 