@HiltAndroidTest
class VoiceServiceIntegrationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule = PermissionTestRule(
        Manifest.permission.RECORD_AUDIO,
        Manifest.permission.INTERNET
    )

    @Inject lateinit var audioRecordingService: AudioRecordingService
    @Inject lateinit var whisperRepository: WhisperRepository
    @Inject lateinit var voiceCommandProcessor: VoiceCommandProcessor
    @Inject lateinit var ttsService: TtsService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testCompleteVoicePipeline() = runBlocking {
        // 1. Record Audio
        val audioFile = audioRecordingService.startRecording()
        delay(3000) // Record for 3 seconds
        audioRecordingService.stopRecording()

        // 2. Transcribe
        val transcriptionResult = whisperRepository.transcribeAudio(audioFile)
        assertThat(transcriptionResult.isSuccess).isTrue()
        
        val transcription = transcriptionResult.getOrNull()
        assertThat(transcription).isNotNull()
        assertThat(transcription).isNotEmpty()

        // 3. Process Command
        voiceCommandProcessor.processCommand(transcription!!)
            .collect { result ->
                when (result) {
                    is CommandResult.Success -> {
                        assertThat(result.response).isNotEmpty()
                        // 4. TTS
                        ttsService.speak(result.response)
                    }
                    is CommandResult.Error -> {
                        fail("Command processing failed: ${result.message}")
                    }
                    is CommandResult.Processing -> {
                        // Expected intermediate state
                    }
                }
            }
    }
} 