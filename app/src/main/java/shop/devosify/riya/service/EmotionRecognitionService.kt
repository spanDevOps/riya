package shop.devosify.riya.service

import android.content.Context
import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmotionRecognitionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gptRepository: GptRepository
) {
    private val faceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .build()
    )

    fun startEmotionDetection(): Flow<EmotionState> = flow {
        // Implement camera preview analysis
        val analyzer = ImageAnalysis.Analyzer { imageProxy ->
            processImage(imageProxy)?.let { emotion ->
                emit(emotion)
            }
        }
    }

    private suspend fun processImage(imageProxy: ImageProxy): EmotionState? {
        val bitmap = imageProxy.toBitmap()
        val faces = faceDetector.process(bitmap)
        
        if (faces.result.isEmpty()) return null

        // Get face region
        val face = faces.result.first()
        val faceBitmap = bitmap.cropFace(face)
        
        // Convert to base64
        val base64Image = faceBitmap.toBase64()

        // Use GPT-4 Vision to analyze emotion
        val prompt = """
            Analyze this facial expression and determine the emotional state.
            Consider: facial muscles, eye expression, mouth position.
            Return format: JSON with 'emotion' and 'confidence' (0-1).
            Be specific about the emotion (e.g., 'mildly frustrated' rather than just 'negative').
        """.trimIndent()

        return gptRepository.generateVisionResponse(base64Image, prompt)
            .map { response ->
                parseEmotionResponse(response)
            }
            .getOrNull()
    }

    private fun Bitmap.toBase64(): String {
        val outputStream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun parseEmotionResponse(response: String): EmotionState {
        // Parse GPT's JSON response
        return try {
            gson.fromJson(response, EmotionState::class.java)
        } catch (e: Exception) {
            EmotionState("neutral", 0.5f)
        }
    }
}

data class EmotionState(
    val emotion: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
) 