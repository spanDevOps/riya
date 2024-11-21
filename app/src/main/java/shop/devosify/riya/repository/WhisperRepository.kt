package shop.devosify.riya.repository

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import shop.devosify.riya.models.WhisperResponse
import shop.devosify.riya.service.RetrofitClient
import shop.devosify.riya.service.WhisperApiService
import java.io.File

class WhisperRepository {

    private val whisperApiService = RetrofitClient.retrofit.create(WhisperApiService::class.java)

    fun transcribeAudio(file: File, callback: (String?) -> Unit) {
        val requestFile = file.asRequestBody("audio/wav".toMediaTypeOrNull())
        val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

        whisperApiService.transcribeAudio(body).enqueue(object : Callback<WhisperResponse> {
            override fun onResponse(call: Call<WhisperResponse>, response: Response<WhisperResponse>) {
                if (response.isSuccessful) {
                    callback(response.body()?.text) // Return the transcription text
                } else {
                    callback(null) // Handle error, possibly display an error message
                }
            }

            override fun onFailure(call: Call<WhisperResponse>, t: Throwable) {
                callback(null) // Handle failure (network error, etc.)
            }
        })
    }
}