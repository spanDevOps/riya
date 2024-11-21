package shop.devosify.riya.service

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import shop.devosify.riya.models.GptRequest
import shop.devosify.riya.models.GptResponse

interface GptApiService {

    @Headers("Content-Type: application/json")
    @POST("v1/completions")
    fun getResponseFromGpt(@Body request: GptRequest): Call<GptResponse>
}
