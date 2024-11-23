package shop.devosify.riya.service

import android.content.Context
import android.util.Base64
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CameraVisionService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gptRepository: GptRepository
) {
    private val cameraExecutor = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private val _currentCamera = MutableStateFlow(CameraFacing.BACK)

    sealed class CameraFacing {
        object FRONT : CameraFacing()
        object BACK : CameraFacing()
    }

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        facing: CameraFacing = CameraFacing.BACK
    ): Flow<VisionAnalysis> = flow {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val cameraSelector = when (facing) {
            is CameraFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            is CameraFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
        }

        imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analysis ->
                analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    processImage(imageProxy).let { visionAnalysis ->
                        emit(visionAnalysis)
                    }
                    imageProxy.close()
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                imageAnalyzer
            )
            _currentCamera.value = facing
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun processImage(imageProxy: ImageProxy): VisionAnalysis {
        val bitmap = imageProxy.toBitmap()
        val base64Image = bitmap.toBase64()

        // Dynamic prompt based on camera facing
        val prompt = when (_currentCamera.value) {
            is CameraFacing.FRONT -> {
                "You are seeing through the front camera. Analyze facial expressions, gestures, and the user's environment. Be ready to provide feedback or suggestions."
            }
            is CameraFacing.BACK -> {
                "You are seeing through the back camera. Describe what you see, identify objects, text, or situations. Be ready to provide relevant information or assistance."
            }
        }

        return gptRepository.generateVisionResponse(base64Image, prompt)
            .map { response ->
                VisionAnalysis(
                    description = response,
                    timestamp = System.currentTimeMillis()
                )
            }
            .getOrDefault(VisionAnalysis("Unable to analyze image", System.currentTimeMillis()))
    }

    fun switchCamera() {
        _currentCamera.value = when (_currentCamera.value) {
            is CameraFacing.FRONT -> CameraFacing.BACK
            is CameraFacing.BACK -> CameraFacing.FRONT
        }
    }

    fun cleanup() {
        cameraExecutor.shutdown()
    }
}

data class VisionAnalysis(
    val description: String,
    val timestamp: Long,
    val objects: List<DetectedObject> = emptyList()
)

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: android.graphics.RectF
) 