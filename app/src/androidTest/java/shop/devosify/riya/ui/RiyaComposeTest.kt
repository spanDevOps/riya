@HiltAndroidTest
class RiyaComposeTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testMicButtonInitialState() {
        composeTestRule.apply {
            onNodeWithContentDescription("Start Recording")
                .assertExists()
                .assertIsDisplayed()
                .assertHasClickAction()
        }
    }

    @Test
    fun testRecordingStateUI() {
        composeTestRule.apply {
            // Click mic button
            onNodeWithContentDescription("Start Recording").performClick()

            // Verify recording state
            onNodeWithContentDescription("Stop Recording")
                .assertExists()
                .assertIsDisplayed()

            // Verify audio visualizer is shown
            onNodeWithTag("AudioVisualizer").assertExists()
        }
    }

    @Test
    fun testCameraPreviewToggle() {
        composeTestRule.apply {
            // Click camera button
            onNodeWithContentDescription("Toggle Camera").performClick()

            // Verify camera preview is shown
            onNodeWithTag("CameraPreview").assertExists()

            // Click again to hide
            onNodeWithContentDescription("Toggle Camera").performClick()

            // Verify camera preview is hidden
            onNodeWithTag("CameraPreview").assertDoesNotExist()
        }
    }

    @Test
    fun testNavigationToHistory() {
        composeTestRule.apply {
            // Click history button
            onNodeWithContentDescription("View History").performClick()

            // Verify we're on history screen
            onNodeWithText("Conversation History").assertExists()
        }
    }
} 