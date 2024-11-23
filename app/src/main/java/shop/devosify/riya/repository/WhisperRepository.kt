package shop.devosify.riya.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import shop.devosify.riya.service.WhisperApiService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WhisperRepository @Inject constructor(
    private val whisperApiService: WhisperApiService
) {
    suspend fun transcribeAudio(file: File): Result<String> = withContext(Dispatchers.IO) {
        try {
            val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)
            
            val response = whisperApiService.transcribeAudio(body)
            if (response.isSuccessful) {
                Result.success(response.body()?.text ?: "")
            } else {
                Result.failure(Exception("Transcription failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}