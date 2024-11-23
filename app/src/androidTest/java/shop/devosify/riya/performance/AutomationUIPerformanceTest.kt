@HiltAndroidTest
class AutomationUIPerformanceTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val benchmarkRule = BenchmarkRule()

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var locationAutomationService: LocationAutomationService
    @Inject lateinit var systemMonitorService: SystemMonitorService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testAutomationScreenLoadPerformance() {
        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("automation_screen_load")

            composeTestRule.apply {
                // Navigate to automation screen
                onNodeWithContentDescription("Settings").performClick()
                onNodeWithText("Automation & Locations").performClick()

                // Wait for screen to load
                onNodeWithText("Active Rules").assertIsDisplayed()

                val metrics = performanceMonitor.endOperation("automation_screen_load")

                // Verify performance
                assertThat(metrics.duration).isLessThan(1000) // Should load within 1 second
                assertThat(metrics.frameDrops).isLessThan(5) // Minimal frame drops
                assertThat(metrics.memoryUsage).isLessThan(50 * 1024 * 1024) // Less than 50MB increase
            }
        }
    }

    @Test
    fun testRuleListScrollPerformance() {
        // Add test rules
        runBlocking {
            repeat(100) { // Test with 100 rules
                locationAutomationService.addRule(createTestRule(it))
            }
        }

        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("rule_list_scroll")

            composeTestRule.apply {
                // Navigate to automation screen
                onNodeWithContentDescription("Settings").performClick()
                onNodeWithText("Automation & Locations").performClick()

                // Perform scroll test
                onNodeWithTag("rules_list").performScrollToIndex(99)

                val metrics = performanceMonitor.endOperation("rule_list_scroll")

                // Verify scroll performance
                assertThat(metrics.frameDrops).isLessThan(5) // Smooth scrolling
                assertThat(metrics.duration).isLessThan(500) // Quick scroll
                assertThat(metrics.cpuUsage).isLessThan(30) // Efficient CPU usage
            }
        }
    }

    @Test
    fun testRuleEditDialogPerformance() {
        val rule = createTestRule(0)
        runBlocking { locationAutomationService.addRule(rule) }

        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("rule_edit_dialog")

            composeTestRule.apply {
                // Navigate and open edit dialog
                onNodeWithContentDescription("Settings").performClick()
                onNodeWithText("Automation & Locations").performClick()
                onNodeWithText(rule.name).performClick()
                onNodeWithText("Edit").performClick()

                // Verify dialog performance
                onNodeWithText("Edit Automation Rule").assertIsDisplayed()

                val metrics = performanceMonitor.endOperation("rule_edit_dialog")

                // Performance assertions
                assertThat(metrics.duration).isLessThan(500) // Quick dialog open
                assertThat(metrics.frameDrops).isZero() // No frame drops
                assertThat(metrics.memoryUsage).isLessThan(10 * 1024 * 1024) // Small memory impact
            }
        }
    }

    @Test
    fun testTabSwitchingPerformance() {
        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("tab_switching")

            composeTestRule.apply {
                // Navigate to automation screen
                onNodeWithContentDescription("Settings").performClick()
                onNodeWithText("Automation & Locations").performClick()

                // Switch tabs multiple times
                repeat(5) {
                    onNodeWithText("Locations").performClick()
                    onNodeWithText("Active Rules").performClick()
                }

                val metrics = performanceMonitor.endOperation("tab_switching")

                // Performance assertions
                assertThat(metrics.duration / 10).isLessThan(100) // Less than 100ms per switch
                assertThat(metrics.frameDrops).isZero() // No frame drops during switching
                assertThat(metrics.memoryUsage).isLessThan(5 * 1024 * 1024) // Minimal memory impact
            }
        }
    }

    @Test
    fun testMemoryLeakCheck() {
        val initialState = systemMonitorService.getCurrentState()
        
        benchmarkRule.measureRepeated {
            composeTestRule.apply {
                repeat(10) { // Navigate back and forth multiple times
                    // Navigate to automation screen
                    onNodeWithContentDescription("Settings").performClick()
                    onNodeWithText("Automation & Locations").performClick()

                    // Perform some actions
                    onNodeWithText("Locations").performClick()
                    onNodeWithText("Active Rules").performClick()

                    // Navigate back
                    onNodeWithContentDescription("Navigate Back").performClick()
                }
            }
        }

        val finalState = systemMonitorService.getCurrentState()
        
        // Verify no significant memory leaks
        val memoryDiff = finalState.memoryUsage - initialState.memoryUsage
        assertThat(memoryDiff).isLessThan(10 * 1024 * 1024) // Less than 10MB growth
    }

    private fun createTestRule(index: Int) = AutomationRule(
        id = "test_rule_$index",
        name = "Test Rule $index",
        type = PatternType.ROUTINE,
        condition = AutomationCondition(
            location = GeofenceType.HOME,
            trigger = TriggerType.ENTER
        ),
        action = AutomationAction(
            type = ActionType.MODIFY_SETTING,
            parameters = mapOf("setting_$index" to "value_$index")
        ),
        confidence = 0.9f
    )
} 