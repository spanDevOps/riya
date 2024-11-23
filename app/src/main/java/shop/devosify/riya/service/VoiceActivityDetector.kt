package shop.devosify.riya.service

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class VoiceActivityDetector @Inject constructor() {
    private val sampleRate = 16000 // Hz
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    // Parameters for VAD
    private val silenceThreshold = 500 // Adjustable threshold for silence
    private val silenceDurationThreshold = 1000L // 1 second of silence to consider speech ended
    private var lastVoiceActivity = 0L

    fun detectSpeechEnd(): Flow<Boolean> = flow {
        var audioRecord: AudioRecord? = null
        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            val buffer = ShortArray(bufferSize)
            audioRecord.startRecording()
            var silenceStartTime = System.currentTimeMillis()

            while (true) {
                val readSize = audioRecord.read(buffer, 0, bufferSize)
                if (readSize > 0) {
                    val energy = calculateEnergy(buffer, readSize)
                    
                    if (energy > silenceThreshold) {
                        // Voice activity detected
                        lastVoiceActivity = System.currentTimeMillis()
                        silenceStartTime = System.currentTimeMillis()
                    } else {
                        // Check if silence duration exceeds threshold
                        val currentSilenceDuration = System.currentTimeMillis() - silenceStartTime
                        if (currentSilenceDuration > silenceDurationThreshold && 
                            lastVoiceActivity > 0) {
                            emit(true) // Speech ended
                            break
                        }
                    }
                }
            }
        } finally {
            audioRecord?.stop()
            audioRecord?.release()
        }
    }

    private fun calculateEnergy(buffer: ShortArray, size: Int): Double {
        var sum = 0.0
        for (i in 0 until size) {
            sum += abs(buffer[i].toDouble())
        }
        return sum / size
    }
} 