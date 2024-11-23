package shop.devosify.riya.service

import android.content.Context
import android.media.MediaPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import shop.devosify.riya.models.TtsRequest
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TtsService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsApiService: TtsApiService
) {
    private var mediaPlayer: MediaPlayer? = null

    suspend fun speak(text: String) {
        try {
            // Use OpenAI's TTS API
            val request = TtsRequest(
                model = "tts-1", // or "tts-1-hd" for higher quality
                text = text,
                voice = "nova" // Options: alloy, echo, fable, onyx, nova, shimmer
            )
            
            val response = ttsApiService.generateSpeech(request)
            if (response.isSuccessful) {
                val audioFile = saveAudioFile(response.body()!!)
                playAudio(audioFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to Android TTS if OpenAI fails
            fallbackToSystemTts(text)
        }
    }

    private suspend fun saveAudioFile(responseBody: ResponseBody): File = withContext(Dispatchers.IO) {
        val file = File(context.cacheDir, "tts_${System.currentTimeMillis()}.mp3")
        FileOutputStream(file).use { outputStream ->
            responseBody.byteStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        file
    }

    private suspend fun playAudio(file: File) = withContext(Dispatchers.IO) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(file.path)
            prepare()
            start()
        }
    }

    private fun fallbackToSystemTts(text: String) {
        // Use Android's built-in TTS as fallback
        android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                it.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
