package shop.devosify.riya.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import shop.devosify.riya.models.GptRequest
import shop.devosify.riya.service.GptApiService
import shop.devosify.riya.utils.SecureStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GptRepository @Inject constructor(
    private val gptApiService: GptApiService,
    private val secureStorage: SecureStorage
) {
    suspend fun generateText(
        prompt: String, 
        forceGpt4: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val useGpt4 = forceGpt4 || secureStorage.getBoolean(PREF_USE_GPT4, false)
            val model = if (useGpt4) {
                "gpt-4-turbo-preview"
            } else {
                "gpt-3.5-turbo"  // Default model
            }
            
            val request = GptRequest(
                model = model,
                prompt = prompt
            )
            
            val response = gptApiService.getResponseFromGpt(request)
            
            if (response.isSuccessful) {
                val text = response.body()?.choices?.firstOrNull()?.text
                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("No text generated"))
                }
            } else {
                Result.failure(Exception("Failed to generate text: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        private const val PREF_USE_GPT4 = "use_gpt4"
    }
}
