package shop.devosify.riya.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import shop.devosify.riya.repository.WhisperRepository
import shop.devosify.riya.record.AudioRecorder

class YourActivity : ComponentActivity() {

    private val audioRecorder = AudioRecorder()
    private val whisperRepository = WhisperRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start recording when needed
        audioRecorder.startRecording()

        // Stop recording after some time (e.g., button press or a timer)
        audioRecorder.stopRecording()

        // Get the file and upload to Whisper
        val audioFile = audioRecorder.getFile()
        whisperRepository.transcribeAudio(audioFile) { transcription ->
            if (transcription != null) {
                // Use the transcription result (e.g., show it in the UI)
            } else {
                // Handle error (e.g., show error message)
            }
        }

        setContent {
            // Your Compose UI setup here
        }
    }
}
