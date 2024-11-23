@RunWith(MockitoJUnitRunner::class)
class VoiceCommandProcessorTest {
    @Mock private lateinit var gptRepository: GptRepository
    @Mock private lateinit var memoryDao: MemoryDao
    @Mock private lateinit var ttsService: TtsService
    @Mock private lateinit var systemMonitorService: SystemMonitorService
    
    private lateinit var voiceCommandProcessor: VoiceCommandProcessor

    @Before
    fun setup() {
        voiceCommandProcessor = VoiceCommandProcessor(
            gptRepository = gptRepository,
            memoryDao = memoryDao,
            ttsService = ttsService,
            systemMonitorService = systemMonitorService
        )
    }

    @Test
    fun `test basic command processing`() = runTest {
        // Given
        val command = "turn on the lights"
        whenever(gptRepository.generateText(any()))
            .thenReturn(Result.success(createMockGptResponse("DEVICE_CONTROL")))

        // When
        val result = voiceCommandProcessor.processCommand(command).first()

        // Then
        assertThat(result).isInstanceOf(CommandResult.Success::class.java)
        verify(memoryDao).insertMemory(any())
    }

    @Test
    fun `test offline command handling`() = runTest {
        // Given
        val command = "what's the battery level"
        whenever(systemMonitorService.getBatteryLevel()).thenReturn(85)

        // When
        val result = voiceCommandProcessor.processCommand(command).first()

        // Then
        assertThat(result).isInstanceOf(CommandResult.Success::class.java)
        assertThat((result as CommandResult.Success).response)
            .contains("85")
    }

    @Test
    fun `test error handling`() = runTest {
        // Given
        val command = "invalid command"
        whenever(gptRepository.generateText(any()))
            .thenReturn(Result.failure(Exception("API Error")))

        // When
        val result = voiceCommandProcessor.processCommand(command).first()

        // Then
        assertThat(result).isInstanceOf(CommandResult.Error::class.java)
    }
} 