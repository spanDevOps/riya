package shop.devosify.riya.models

data class TtsRequest(
    val model: String = "tts-1",
    val text: String,
    val voice: String = "nova",
    val speed: Float = 1.0f,
    val format: String = "mp3"
)