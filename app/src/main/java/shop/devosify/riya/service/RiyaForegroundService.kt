package shop.devosify.riya.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject
import java.io.File

@AndroidEntryPoint
class RiyaForegroundService : Service() {
    @Inject lateinit var wakeWordService: WakeWordService
    @Inject lateinit var systemMonitorService: SystemMonitorService
    @Inject lateinit var voiceCommandProcessor: VoiceCommandProcessor
    @Inject lateinit var whisperRepository: WhisperRepository
    @Inject lateinit var voiceActivityDetector: VoiceActivityDetector
    @Inject lateinit var audioRecordingService: AudioRecordingService
    @Inject lateinit var ttsService: TtsService
    @Inject lateinit var analyticsService: AnalyticsService
    
    private var wakeLock: PowerManager.WakeLock? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        acquireWakeLock()
        startListening()
    }

    private fun startListening() {
        serviceScope.launch {
            try {
                wakeWordService.startListening { 
                    // Wake word detected
                    handleWakeWord()
                }.collect { wakeWordDetected ->
                    if (wakeWordDetected) {
                        ttsService.speak("Yes?") // Optional acknowledgment
                    }
                }
            } catch (e: Exception) {
                analyticsService.logError("wake_word_listening", e.message ?: "Unknown error")
                // Retry after delay
                kotlinx.coroutines.delay(1000)
                startListening()
            }
        }
    }

    private suspend fun handleWakeWord() {
        try {
            val audioFile = audioRecordingService.startRecording()
            
            // Start collecting audio visualization data
            serviceScope.launch {
                audioRecordingService.getAmplitude().collect { amplitude ->
                    // Update visualization if app is in foreground
                }
            }
            
            // Wait for speech to end
            voiceActivityDetector.detectSpeechEnd().collect { speechEnded ->
                if (speechEnded) {
                    audioRecordingService.stopRecording()?.let { file ->
                        processVoiceCommand(file)
                    }
                }
            }
        } catch (e: Exception) {
            analyticsService.logError("wake_word_handling", e.message ?: "Unknown error")
            ttsService.speak("Sorry, I encountered an error")
        }
    }

    private suspend fun processVoiceCommand(audioFile: File) {
        whisperRepository.transcribeAudio(audioFile)
            .onSuccess { transcription ->
                voiceCommandProcessor.processCommand(transcription)
                    .collect { result ->
                        when (result) {
                            is CommandResult.Success -> {
                                ttsService.speak(result.response)
                            }
                            is CommandResult.Error -> {
                                ttsService.speak("Sorry, I couldn't process that command")
                            }
                            is CommandResult.Processing -> {
                                // Optional processing feedback
                            }
                        }
                    }
            }
            .onFailure {
                ttsService.speak("Sorry, I couldn't understand what you said")
            }
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "Riya::ListeningWakeLock"
        ).apply {
            acquire(10*60*1000L /*10 minutes*/)
        }
    }

    companion object {
        private const val NOTIFICATION_ID = 1
    }
} 