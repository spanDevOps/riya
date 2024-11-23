package shop.devosify.riya.service

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.picovoice.porcupine.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WakeWordService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private var porcupine: Porcupine? = null
    private var audioRecord: AudioRecord? = null
    private var isListening = false

    private val frameLength: Int
        get() = porcupine?.frameLength ?: 0

    private val sampleRate: Int
        get() = porcupine?.sampleRate ?: 16000

    fun startListening(onWakeWord: () -> Unit): Flow<Boolean> = flow {
        try {
            // Initialize Porcupine with "Hey Riya" wake word
            porcupine = Porcupine.Builder()
                .setAccessKey(BuildConfig.PORCUPINE_KEY)
                .setKeyword("Hey Riya")
                .build(context)

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build()

            val bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val readBuffer = ShortArray(frameLength)
            audioRecord?.startRecording()
            isListening = true

            while (isListening) {
                val readSamples = audioRecord?.read(readBuffer, 0, frameLength) ?: 0
                if (readSamples == frameLength) {
                    val wakeWordDetected = porcupine?.process(readBuffer) ?: -1
                    if (wakeWordDetected >= 0) {
                        emit(true)
                        onWakeWord()
                    }
                }
            }
        } finally {
            stopListening()
        }
    }

    fun stopListening() {
        isListening = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        porcupine?.delete()
        porcupine = null
    }

    companion object {
        private const val PORCUPINE_ACCESS_KEY = "your_porcupine_access_key" // Get from Picovoice Console
    }
} 