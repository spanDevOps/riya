package shop.devosify.riya.service


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import shop.devosify.riya.models.ContextRequest

interface AssistantsApiService {

    // Endpoint to send context to the Assistants API
    @POST("v1/assistants/context") // Update with your actual API endpoint
    fun sendContext(@Body contextRequest: ContextRequest): Call<Void>
}
