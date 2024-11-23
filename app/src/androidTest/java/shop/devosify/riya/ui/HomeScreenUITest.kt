@HiltAndroidTest
class HomeScreenUITest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @get:Rule(order = 2)
    val permissionRule = PermissionTestRule(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.CAMERA
    )

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testVoiceInteractionFlow() {
        composeTestRule.apply {
            // Initial state
            onNodeWithContentDescription("Start Recording")
                .assertExists()
                .assertIsDisplayed()
                .performClick()

            // Recording state
            onNodeWithContentDescription("Stop Recording")
                .assertExists()
                .assertIsDisplayed()

            onNodeWithTag("AudioVisualizer")
                .assertExists()
                .assertIsDisplayed()

            // Stop recording
            onNodeWithContentDescription("Stop Recording")
                .performClick()

            // Wait for response
            waitUntil(timeoutMillis = 5000) {
                onAllNodesWithTag("ResponseText")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Verify response
            onNodeWithTag("ResponseText")
                .assertExists()
                .assertIsDisplayed()
                .assertTextContains("I heard you")
        }
    }

    @Test
    fun testCameraInteraction() {
        composeTestRule.apply {
            // Initial state - camera off
            onNodeWithTag("CameraPreview")
                .assertDoesNotExist()

            // Toggle camera on
            onNodeWithContentDescription("Toggle Camera")
                .performClick()

            // Verify camera preview
            onNodeWithTag("CameraPreview")
                .assertExists()
                .assertIsDisplayed()

            // Wait for vision analysis
            waitUntil(timeoutMillis = 3000) {
                onAllNodesWithTag("VisionAnalysis")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Verify vision analysis
            onNodeWithTag("VisionAnalysis")
                .assertExists()
                .assertIsDisplayed()

            // Toggle camera off
            onNodeWithContentDescription("Toggle Camera")
                .performClick()

            // Verify camera preview is gone
            onNodeWithTag("CameraPreview")
                .assertDoesNotExist()
        }
    }

    @Test
    fun testNavigationAndHistory() {
        composeTestRule.apply {
            // Navigate to history
            onNodeWithContentDescription("View History")
                .performClick()

            // Verify history screen
            onNodeWithText("Conversation History")
                .assertExists()
                .assertIsDisplayed()

            // Check history items
            onAllNodesWithTag("HistoryItem")
                .assertCountAtLeast(1)

            // Navigate back
            onNodeWithContentDescription("Navigate Back")
                .performClick()

            // Verify back on home screen
            onNodeWithContentDescription("Start Recording")
                .assertExists()
                .assertIsDisplayed()
        }
    }
} 