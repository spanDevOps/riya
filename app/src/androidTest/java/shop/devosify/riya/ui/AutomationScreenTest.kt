@HiltAndroidTest
class AutomationScreenTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject lateinit var locationAutomationService: LocationAutomationService
    @Inject lateinit var placesService: PlacesService
    @Inject lateinit var analyticsService: AnalyticsService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testAutomationScreenNavigation() {
        // Navigate to automation screen
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        composeTestRule.onNodeWithText("Automation & Locations")
            .performClick()

        // Verify screen title
        composeTestRule.onNodeWithText("Automation & Locations")
            .assertIsDisplayed()

        // Verify tabs
        composeTestRule.onNodeWithText("Active Rules")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Locations")
            .assertIsDisplayed()
    }

    @Test
    fun testRuleToggle() {
        // Add test rule
        val rule = createTestRule()
        runBlocking { locationAutomationService.addRule(rule) }

        navigateToAutomationScreen()

        // Find and toggle rule
        composeTestRule.onNodeWithText(rule.name)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag("rule_toggle_${rule.id}")
            .performClick()

        // Verify toggle state changed
        composeTestRule.onNodeWithTag("rule_toggle_${rule.id}")
            .assertIsToggled()
    }

    @Test
    fun testRuleEditing() {
        val rule = createTestRule()
        runBlocking { locationAutomationService.addRule(rule) }

        navigateToAutomationScreen()

        // Open edit dialog
        composeTestRule.onNodeWithText(rule.name)
            .performClick()
        composeTestRule.onNodeWithText("Edit")
            .performClick()

        // Verify edit dialog
        composeTestRule.onNodeWithText("Edit Automation Rule")
            .assertIsDisplayed()

        // Edit rule name
        composeTestRule.onNodeWithTag("rule_name_input")
            .performTextReplacement("Updated Rule Name")

        // Save changes
        composeTestRule.onNodeWithText("Save")
            .performClick()

        // Verify updates
        composeTestRule.onNodeWithText("Updated Rule Name")
            .assertIsDisplayed()
    }

    @Test
    fun testLocationManagement() {
        // Switch to Locations tab
        composeTestRule.onNodeWithText("Locations")
            .performClick()

        // Add test location
        val location = createTestLocation()
        runBlocking { placesService.addLocation(location) }

        // Verify location displayed
        composeTestRule.onNodeWithText(location.name)
            .assertIsDisplayed()

        // Edit location
        composeTestRule.onNodeWithText(location.name)
            .performClick()
        composeTestRule.onNodeWithText("Edit")
            .performClick()

        // Update location name
        composeTestRule.onNodeWithTag("location_name_input")
            .performTextReplacement("Updated Location Name")

        // Save changes
        composeTestRule.onNodeWithText("Save")
            .performClick()

        // Verify updates
        composeTestRule.onNodeWithText("Updated Location Name")
            .assertIsDisplayed()
    }

    @Test
    fun testRuleDeletion() {
        val rule = createTestRule()
        runBlocking { locationAutomationService.addRule(rule) }

        navigateToAutomationScreen()

        // Delete rule
        composeTestRule.onNodeWithText(rule.name)
            .performClick()
        composeTestRule.onNodeWithText("Delete")
            .performClick()

        // Confirm deletion
        composeTestRule.onNodeWithText("Confirm")
            .performClick()

        // Verify rule removed
        composeTestRule.onNodeWithText(rule.name)
            .assertDoesNotExist()
    }

    @Test
    fun testErrorHandling() {
        // Simulate error state
        runBlocking {
            locationAutomationService.simulateError()
        }

        navigateToAutomationScreen()

        // Verify error state
        composeTestRule.onNodeWithText("Failed to load automation data")
            .assertIsDisplayed()

        // Test retry
        composeTestRule.onNodeWithText("Retry")
            .performClick()

        // Verify recovery
        composeTestRule.onNodeWithText("Active Rules")
            .assertIsDisplayed()
    }

    private fun navigateToAutomationScreen() {
        composeTestRule.onNodeWithContentDescription("Settings")
            .performClick()
        composeTestRule.onNodeWithText("Automation & Locations")
            .performClick()
    }

    private fun createTestRule() = AutomationRule(
        id = UUID.randomUUID().toString(),
        name = "Test Rule",
        description = "Test Description",
        type = PatternType.ROUTINE,
        condition = AutomationCondition(
            location = GeofenceType.HOME,
            trigger = TriggerType.ENTER
        ),
        action = AutomationAction(
            type = ActionType.MODIFY_SETTING,
            parameters = mapOf()
        ),
        confidence = 0.9f
    )

    private fun createTestLocation() = LocationData(
        id = UUID.randomUUID().toString(),
        name = "Test Location",
        type = GeofenceType.FREQUENT,
        latitude = 37.4220,
        longitude = -122.0841
    )
} 