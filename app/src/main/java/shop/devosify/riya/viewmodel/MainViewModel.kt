package shop.devosify.riya.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import shop.devosify.riya.repository.AssistantsRepository
import shop.devosify.riya.repository.GptRepository
import shop.devosify.riya.repository.WhisperRepository
import shop.devosify.riya.service.AudioRecordingService
import shop.devosify.riya.service.AudioVisualizerService
import shop.devosify.riya.service.TtsService
import java.io.File
import javax.inject.Inject
import shop.devosify.riya.util.NetworkUtils
import shop.devosify.riya.service.MemoryExtractionService
import shop.devosify.riya.data.local.entity.MemoryEntity
import shop.devosify.riya.data.local.dao.MemoryDao
import shop.devosify.riya.service.EmotionRecognitionService
import shop.devosify.riya.service.CameraVisionService

@HiltViewModel
class MainViewModel @Inject constructor(
    private val audioRecordingService: AudioRecordingService,
    private val whisperRepository: WhisperRepository,
    private val assistantsRepository: AssistantsRepository,
    private val gptRepository: GptRepository,
    private val ttsService: TtsService,
    private val audioVisualizerService: AudioVisualizerService,
    private val conversationRepository: ConversationRepository,
    private val analyticsService: AnalyticsService,
    private val networkUtils: NetworkUtils,
    private val memoryExtractionService: MemoryExtractionService,
    private val memoryDao: MemoryDao,
    private val emotionRecognitionService: EmotionRecognitionService,
    private val cameraVisionService: CameraVisionService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private var currentAudioFile: File? = null
    private var currentThreadId: String? = null
    private var currentAssistantId: String? = null
    private var currentConversationId: Long? = null

    private val _audioAmplitudes = MutableStateFlow<List<Float>>(List(30) { 0f })
    val audioAmplitudes: StateFlow<List<Float>> = _audioAmplitudes.asStateFlow()

    private var interactionStartTime: Long = 0
    private var currentMessageCount: Int = 0

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _emotionState = MutableStateFlow<EmotionState?>(null)
    val emotionState: StateFlow<EmotionState?> = _emotionState.asStateFlow()

    private val _cameraVision = MutableStateFlow<VisionAnalysis?>(null)
    val cameraVision: StateFlow<VisionAnalysis?> = _cameraVision.asStateFlow()

    init {
        viewModelScope.launch {
            networkUtils.observeNetworkStatus()
                .collect { isOnline ->
                    _isOnline.value = isOnline
                }
            setupAssistant()
            startEmotionRecognition()
        }
    }

    private suspend fun setupAssistant() {
        val assistantResult = assistantsRepository.createAssistant(
            name = "Riya",
            instructions = "You are Riya, a helpful and intelligent AI assistant. " +
                    "You should provide concise, accurate responses and be proactive in helping users."
        )
        
        assistantResult.onSuccess { assistant ->
            currentAssistantId = assistant.id
            analyticsService.logAssistantCreation(true)
            createNewThread()
        }.onFailure {
            analyticsService.logAssistantCreation(false)
            analyticsService.logError(
                errorType = "assistant_creation",
                errorMessage = it.message ?: "Unknown error",
                stackTrace = it.stackTraceToString()
            )
            _uiState.value = UiState.Error("Failed to create assistant: ${it.message}")
        }
    }

    private suspend fun createNewThread() {
        val threadResult = assistantsRepository.createThread()
        threadResult.onSuccess { thread ->
            currentThreadId = thread.id
            currentConversationId = conversationRepository.createConversation(thread.id)
            _uiState.value = UiState.Idle
        }.onFailure {
            _uiState.value = UiState.Error("Failed to create thread: ${it.message}")
        }
    }

    fun startRecording() {
        interactionStartTime = System.currentTimeMillis()
        analyticsService.logConversationStarted()
        
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Recording
                currentAudioFile = audioRecordingService.startRecording()
                
                // Start collecting amplitudes
                audioVisualizerService.startVisualization()
                    .collect { amplitude ->
                        val currentAmplitudes = _audioAmplitudes.value.toMutableList()
                        currentAmplitudes.removeAt(0)
                        currentAmplitudes.add(amplitude)
                        _audioAmplitudes.value = currentAmplitudes
                    }
            } catch (e: Exception) {
                analyticsService.logError(
                    errorType = "recording_start",
                    errorMessage = e.message ?: "Unknown error",
                    stackTrace = e.stackTraceToString()
                )
                _uiState.value = UiState.Error("Failed to start recording: ${e.message}")
            }
        }
    }

    fun stopRecordingAndProcess() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Processing
                audioVisualizerService.stopVisualization()
                val audioFile = audioRecordingService.stopRecording()
                if (audioFile != null) {
                    processAudioFile(audioFile)
                } else {
                    _uiState.value = UiState.Error("No audio recorded")
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Failed to process recording: ${e.message}")
            }
        }
    }

    private suspend fun processAudioFile(file: File) {
        whisperRepository.transcribeAudio(file)
            .onSuccess { transcription ->
                if (transcription.isNotBlank()) {
                    analyticsService.logVoiceInteraction(
                        transcriptionSuccess = true,
                        responseSuccess = false, // Will be updated after response
                        duration = System.currentTimeMillis() - interactionStartTime
                    )
                    processUserInput(transcription)
                } else {
                    analyticsService.logVoiceInteraction(
                        transcriptionSuccess = false,
                        responseSuccess = false,
                        duration = System.currentTimeMillis() - interactionStartTime
                    )
                    _uiState.value = UiState.Error("No speech detected")
                }
            }.onFailure {
                analyticsService.logError(
                    errorType = "transcription",
                    errorMessage = it.message ?: "Unknown error",
                    stackTrace = it.stackTraceToString()
                )
                _uiState.value = UiState.Error("Transcription failed: ${it.message}")
            }
    }

    private suspend fun processUserInput(input: String) {
        currentThreadId?.let { threadId ->
            currentAssistantId?.let { assistantId ->
                currentConversationId?.let { conversationId ->
                    _uiState.value = UiState.Processing
                    currentMessageCount++
                    
                    // Save user message locally
                    conversationRepository.addMessage(conversationId, input, isUser = true)
                    
                    if (!networkUtils.isNetworkAvailable()) {
                        // Notify user via voice about offline status
                        ttsService.speak(
                            "I'm currently offline. I'll process your request when I'm back online."
                        )
                        
                        // Queue the action for later processing
                        offlineManager.queueOfflineAction(
                            OfflineAction(
                                type = ActionType.VOICE_COMMAND,
                                data = mapOf(
                                    "input" to input,
                                    "threadId" to threadId,
                                    "assistantId" to assistantId,
                                    "conversationId" to conversationId
                                )
                            )
                        )
                        
                        _uiState.value = UiState.Success(
                            "I'm currently offline. Your message has been saved and will be processed when online."
                        )
                        return
                    }

                    assistantsRepository.sendMessage(threadId, input)
                        .onSuccess {
                            assistantsRepository.createAndWaitForRun(threadId, assistantId)
                                .onSuccess { run ->
                                    val response = run.toString()
                                    
                                    // Extract and store memories
                                    val conversation = "User: $input\nAssistant: $response"
                                    memoryExtractionService.extractMemories(conversation)
                                        .onSuccess { memories ->
                                            memories.forEach { memory ->
                                                memoryDao.insertMemory(memory)
                                            }
                                        }
                                    
                                    conversationRepository.addMessage(
                                        conversationId, 
                                        response, 
                                        isUser = false
                                    )
                                    _uiState.value = UiState.Success(response)
                                    ttsService.speak(response)
                                    
                                    analyticsService.logVoiceInteraction(
                                        transcriptionSuccess = true,
                                        responseSuccess = true,
                                        duration = System.currentTimeMillis() - interactionStartTime
                                    )
                                }.onFailure {
                                    handleError("response_generation", it)
                                }
                        }.onFailure {
                            handleError("message_send", it)
                        }
                }
            }
        } ?: run {
            handleError("initialization", Exception("Assistant not initialized"))
        }
    }

    private fun handleError(errorType: String, error: Throwable) {
        analyticsService.logError(
            errorType = errorType,
            errorMessage = error.message ?: "Unknown error",
            stackTrace = error.stackTraceToString()
        )
        _uiState.value = UiState.Error(error.message ?: "Unknown error")
    }

    fun retry() {
        viewModelScope.launch {
            when (val currentState = _uiState.value) {
                is UiState.Error -> {
                    when {
                        currentAssistantId == null -> setupAssistant()
                        currentThreadId == null -> createNewThread()
                        currentState.message.contains("transcription") -> {
                            currentAudioFile?.let { processAudioFile(it) }
                        }
                        currentState.message.contains("recording") -> {
                            startRecording()
                        }
                        else -> _uiState.value = UiState.Idle
                    }
                }
                else -> {} // Do nothing if not in error state
            }
        }
    }

    fun cancelOperation() {
        viewModelScope.launch {
            when (_uiState.value) {
                is UiState.Recording -> {
                    audioRecordingService.stopRecording()
                    analyticsService.logConversationEnded(
                        messageCount = currentMessageCount,
                        duration = System.currentTimeMillis() - interactionStartTime
                    )
                    _uiState.value = UiState.Idle
                }
                is UiState.Processing -> {
                    _uiState.value = UiState.Idle
                }
                else -> {}
            }
        }
    }

    private fun startEmotionRecognition() {
        viewModelScope.launch {
            emotionRecognitionService.startEmotionDetection()
                .collect { emotion ->
                    _emotionState.value = emotion
                    // Adjust assistant's responses based on emotion
                    adjustResponseStyle(emotion)
                }
        }
    }

    private fun adjustResponseStyle(emotion: EmotionState) {
        // Update assistant's instruction based on user's emotional state
        val emotionalContext = when {
            emotion.emotion.contains("frustrated") -> 
                "User seems frustrated. Be extra patient and helpful."
            emotion.emotion.contains("happy") -> 
                "User is in a good mood. Match their positive energy."
            emotion.emotion.contains("confused") -> 
                "User seems confused. Provide simpler, step-by-step explanations."
            else -> null
        }

        emotionalContext?.let { context ->
            viewModelScope.launch {
                currentAssistantId?.let { assistantId ->
                    assistantsRepository.updateAssistantInstructions(
                        assistantId,
                        "... existing instructions ... \n\nCurrent context: $context"
                    )
                }
            }
        }
    }

    fun startCameraVision(lifecycleOwner: LifecycleOwner) {
        viewModelScope.launch {
            cameraVisionService.startCamera(lifecycleOwner)
                .collect { analysis ->
                    _cameraVision.value = analysis
                    // Optionally speak the analysis
                    if (analysis.description.isNotBlank()) {
                        ttsService.speak(analysis.description)
                    }
                }
        }
    }

    fun switchCamera() {
        cameraVisionService.switchCamera()
    }

    sealed class UiState {
        object Idle : UiState()
        object Recording : UiState()
        object Processing : UiState()
        data class Success(val response: String) : UiState()
        data class Error(val message: String) : UiState()
    }
} 