@HiltAndroidTest
class AssistantIntegrationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var voiceCommandProcessor: VoiceCommandProcessor
    @Inject lateinit var memorySystem: MemorySystem
    @Inject lateinit var contextManager: ContextManager
    @Inject lateinit var serviceCoordinator: ServiceCoordinator

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testCompleteAssistantFlow() = runTest {
        // Test voice command flow
        val commandResult = voiceCommandProcessor.processCommand("what's the weather")
            .first()
        assertThat(commandResult).isInstanceOf(CommandResult.Success::class.java)

        // Verify memory storage
        val memories = memorySystem.getRecentMemories()
        assertThat(memories).isNotEmpty()
        assertThat(memories.first().content).contains("weather")

        // Check context update
        val context = contextManager.getCurrentContext()
        assertThat(context.lastQuery).isEqualTo("what's the weather")
    }

    @Test
    fun testOfflineCapabilities() = runTest {
        // Simulate offline mode
        networkUtils.setNetworkAvailable(false)

        // Test basic command
        val result = voiceCommandProcessor.processCommand("turn on lights")
            .first()
        assertThat(result).isInstanceOf(CommandResult.Success::class.java)

        // Verify local processing
        verify(gptRepository, never()).generateText(any())
    }
} 