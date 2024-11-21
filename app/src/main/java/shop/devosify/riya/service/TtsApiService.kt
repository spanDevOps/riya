package shop.devosify.riya.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import shop.devosify.riya.models.TtsRequest
import shop.devosify.riya.models.TtsResponse

interface TtsApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/audio/generate")
    fun generateSpeech(@Body request: TtsRequest): Call<TtsResponse>
}
