package shop.devosify.riya.models

data class GptRequest(
    val model: String = "gpt-3.5-turbo",
    val prompt: String,
    val max_tokens: Int = 100
)