package shop.devosify.riya.service

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.POST
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Headers
import shop.devosify.riya.models.WhisperResponse

interface WhisperApiService {

    @Headers("Authorization: Bearer openai_api_key")
    @Multipart
    @POST("v1/audio/transcriptions")
    fun transcribeAudio(
        @Part audio: MultipartBody.Part,
        @Part("model") model: String = "whisper-1",
        @Part("language") language: String = "en"
    ): Call<WhisperResponse> // Changed from ResponseBody to WhisperResponse
}
