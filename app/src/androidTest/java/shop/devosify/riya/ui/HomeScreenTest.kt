@HiltAndroidTest
class HomeScreenTest {
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
    fun testInitialState() {
        composeTestRule.apply {
            // Verify initial UI elements
            onNodeWithContentDescription("Start Recording")
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()

            onNodeWithContentDescription("Toggle Camera")
                .assertExists()
                .assertIsDisplayed()

            onNodeWithContentDescription("View History")
                .assertExists()
                .assertIsDisplayed()
        }
    }

    @Test
    fun testRecordingFlow() {
        composeTestRule.apply {
            // Start recording
            onNodeWithContentDescription("Start Recording").performClick()
            
            // Verify recording state
            onNodeWithContentDescription("Stop Recording")
                .assertExists()
                .assertIsDisplayed()

            onNodeWithTag("AudioVisualizer")
                .assertExists()
                .assertIsDisplayed()

            // Stop recording
            onNodeWithContentDescription("Stop Recording").performClick()

            // Verify processing state
            onNodeWithTag("ProcessingIndicator")
                .assertExists()
                .assertIsDisplayed()

            // Wait for response
            waitUntil(timeoutMillis = 5000) {
                onAllNodesWithTag("ResponseText")
                    .fetchSemanticsNodes().isNotEmpty()
            }

            // Verify response
            onNodeWithTag("ResponseText")
                .assertExists()
                .assertIsDisplayed()
        }
    }
} 