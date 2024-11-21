package shop.devosify.riya.record

import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class AudioRecorder {

    private var mediaRecorder: MediaRecorder? = null
    private var filePath: String? = null

    fun startRecording() {
        filePath = "/path/to/audio_file.wav" // You can change this path as needed
        mediaRecorder = MediaRecorder()
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.WAV)
            setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
            setOutputFile(filePath)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun stopRecording() {
        mediaRecorder?.stop()
        mediaRecorder?.release()
    }

    fun getFile(): File {
        return File(filePath ?: throw IllegalStateException("File path is null"))
    }
}
