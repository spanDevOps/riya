@HiltAndroidTest
class AutomationSystemTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule = PermissionTestRule(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_BACKGROUND_LOCATION
    )

    @Inject lateinit var locationAutomationService: LocationAutomationService
    @Inject lateinit var placesService: PlacesService
    @Inject lateinit var patternAnalysisService: PatternAnalysisService
    @Inject lateinit var geofencingService: GeofencingService
    @Inject lateinit var memoryDao: MemoryDao

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testAutomationRuleCreation() = runTest {
        // Create test rule
        val rule = AutomationRule(
            id = UUID.randomUUID().toString(),
            type = PatternType.ROUTINE,
            condition = AutomationCondition(
                location = GeofenceType.HOME,
                timeRange = TimeRange(
                    start = System.currentTimeMillis(),
                    end = System.currentTimeMillis() + 3600000 // 1 hour
                ),
                trigger = TriggerType.ENTER
            ),
            action = AutomationAction(
                type = ActionType.MODIFY_SETTING,
                parameters = mapOf(
                    "setting" to "wifi",
                    "value" to "on"
                )
            ),
            confidence = 0.9f
        )

        // Add rule
        locationAutomationService.addRule(rule)

        // Verify rule was added
        val activeRules = locationAutomationService.activeRules.first()
        assertThat(activeRules).contains(rule)
    }

    @Test
    fun testLocationBasedTrigger() = runTest {
        // Create test location
        val location = GeofenceData(
            id = "test_home",
            name = "Home",
            latitude = 37.4220,
            longitude = -122.0841,
            radius = 100f,
            type = GeofenceType.HOME
        )

        // Setup geofence
        geofencingService.setupGeofencesForFrequentPlaces()

        // Simulate location entry
        val context = locationAutomationService.handleLocationTrigger(
            location = location,
            trigger = TriggerType.ENTER
        )

        // Verify automation executed
        assertThat(context.executedRules).isNotEmpty()
        assertThat(context.success).isTrue()

        // Verify memory creation
        val memories = memoryDao.getMemoriesByTag("automation").first()
        assertThat(memories).isNotEmpty()
        assertThat(memories.first().content).contains("Home")
    }

    @Test
    fun testPatternBasedAutomation() = runTest {
        // Generate test patterns
        val patterns = patternAnalysisService.getUserPatterns().first()

        // Verify pattern detection
        assertThat(patterns).isNotEmpty()
        
        // Create automation from pattern
        val highConfidencePattern = patterns.first { it.confidence > 0.8f }
        val rule = locationAutomationService.createRuleFromPattern(highConfidencePattern)

        // Verify rule creation
        assertThat(rule).isNotNull()
        assertThat(rule.confidence).isGreaterThan(0.8f)

        // Test rule execution
        val result = locationAutomationService.executeRule(rule)
        assertThat(result.success).isTrue()
    }

    @Test
    fun testAutomationPriorities() = runTest {
        // Create multiple rules with different priorities
        val rules = listOf(
            createTestRule(priority = 1),
            createTestRule(priority = 5),
            createTestRule(priority = 3)
        )

        rules.forEach { locationAutomationService.addRule(it) }

        // Trigger automation
        val location = createTestLocation()
        val context = locationAutomationService.handleLocationTrigger(
            location = location,
            trigger = TriggerType.ENTER
        )

        // Verify execution order
        assertThat(context.executedRules).isOrdered { a, b ->
            getRulePriority(a) >= getRulePriority(b)
        }
    }

    @Test
    fun testAutomationConflictResolution() = runTest {
        // Create conflicting rules
        val rule1 = createTestRule(
            action = AutomationAction(
                type = ActionType.MODIFY_SETTING,
                parameters = mapOf("wifi" to "on")
            )
        )
        val rule2 = createTestRule(
            action = AutomationAction(
                type = ActionType.MODIFY_SETTING,
                parameters = mapOf("wifi" to "off")
            )
        )

        locationAutomationService.addRule(rule1)
        locationAutomationService.addRule(rule2)

        // Verify conflict resolution
        val conflicts = locationAutomationService.detectConflicts()
        assertThat(conflicts).isNotEmpty()
        assertThat(conflicts.first().rules).containsExactly(rule1, rule2)
    }

    private fun createTestRule(
        priority: Int = 3,
        action: AutomationAction = AutomationAction(
            type = ActionType.MODIFY_SETTING,
            parameters = mapOf()
        )
    ): AutomationRule {
        return AutomationRule(
            id = UUID.randomUUID().toString(),
            type = PatternType.ROUTINE,
            condition = AutomationCondition(
                location = GeofenceType.HOME,
                trigger = TriggerType.ENTER
            ),
            action = action,
            confidence = 0.9f,
            priority = priority
        )
    }

    private fun createTestLocation(): GeofenceData {
        return GeofenceData(
            id = "test_location",
            name = "Test Location",
            latitude = 37.4220,
            longitude = -122.0841,
            radius = 100f,
            type = GeofenceType.FREQUENT
        )
    }

    private fun getRulePriority(ruleId: String): Int {
        return locationAutomationService.activeRules.value
            .find { it.id == ruleId }
            ?.priority ?: 0
    }
} 