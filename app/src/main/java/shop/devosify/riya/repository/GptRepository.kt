package shop.devosify.riya.repository

import shop.devosify.riya.models.GptRequest
import shop.devosify.riya.models.GptResponse
import shop.devosify.riya.service.RetrofitClient
import shop.devosify.riya.service.GptApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GptRepository {

    private val gptApiService = RetrofitClient.gptRetrofit.create(GptApiService::class.java)

    fun generateText(prompt: String, callback: (String?) -> Unit) {
        val gptRequest = GptRequest(prompt = prompt)

        gptApiService.getResponseFromGpt(gptRequest).enqueue(object : Callback<GptResponse> {
            override fun onResponse(call: Call<GptResponse>, response: Response<GptResponse>) {
                if (response.isSuccessful) {
                    callback(response.body()?.choices?.firstOrNull()?.text) // Return generated text
                } else {
                    callback(null) // Handle error
                }
            }

            override fun onFailure(call: Call<GptResponse>, t: Throwable) {
                callback(null) // Handle failure
            }
        })
    }
}
