package shop.devosify.riya.service

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface WhisperApiService {
    @Multipart
    @POST("v1/audio/transcriptions")
    @Headers("Content-Type: multipart/form-data")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: MultipartBody.Part = MultipartBody.Part.createFormData("model", "whisper-1"),
        @Part("language") language: MultipartBody.Part = MultipartBody.Part.createFormData("language", "en")
    ): Response<TranscriptionResponse>
}

data class TranscriptionResponse(
    val text: String
)
