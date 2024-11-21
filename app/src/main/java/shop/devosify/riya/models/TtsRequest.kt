package shop.devosify.riya.models

data class TtsRequest(
    val model: String = "text-to-speech-v1",
    val text: String
)