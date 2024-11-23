package shop.devosify.riya.service

import android.content.Context
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
class AudioRecordingService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val voiceActivityDetector: VoiceActivityDetector
) {
    private var mediaRecorder: MediaRecorder? = null
    private var currentFile: File? = null
    private var isRecording = false

    fun startRecording(): File {
        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.wav")
        currentFile = file
        
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            setAudioSamplingRate(16000) // Match VAD sample rate
            setAudioChannels(1) // Mono
            prepare()
            start()
        }
        
        isRecording = true
        return file
    }

    fun stopRecording(): File? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            currentFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun isRecording(): Boolean = isRecording

    // Stream audio amplitude for visualization
    fun getAmplitude(): Flow<Float> = flow {
        while (isRecording) {
            val amplitude = mediaRecorder?.maxAmplitude ?: 0
            // Normalize amplitude to 0-1 range
            emit(amplitude.toFloat() / Short.MAX_VALUE)
            kotlinx.coroutines.delay(50) // 20fps update rate
        }
    }

    // Cleanup temporary files
    fun cleanup() {
        currentFile?.delete()
        currentFile = null
    }
} 