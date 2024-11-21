package shop.devosify.riya.repository

import shop.devosify.riya.models.ContextRequest
import shop.devosify.riya.service.RetrofitClient
import shop.devosify.riya.service.AssistantsApiService
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

// The AssistantsRepository handles sending context to the Assistants API
class AssistantsRepository {

    // Creating the AssistantsApiService from Retrofit
    private val assistantsApiService = RetrofitClient.assistantsRetrofit.create(AssistantsApiService::class.java)

    // Method to send context to the Assistants API
    fun sendContext(context: String, message: String, userId: String, callback: (Boolean) -> Unit) {
        // Creating a data model for the context, message, and user_id to be sent to the Assistants API
        val assistantContext = ContextRequest(
            context = context,
            message = message,   // Pass the message content
            user_id = userId     // Pass the user ID
        )

        // Sending a POST request to the Assistants API to update context
        assistantsApiService.sendContext(assistantContext).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback(true)  // Context sent successfully
                } else {
                    callback(false) // Error in sending context
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                callback(false)  // Failure in making the request
            }
        })
    }
}
