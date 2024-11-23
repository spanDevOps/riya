package shop.devosify.riya.service

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import shop.devosify.riya.models.TtsRequest

interface TtsApiService {
    @POST("v1/audio/speech")
    suspend fun generateSpeech(@Body request: TtsRequest): Response<ResponseBody>
}
