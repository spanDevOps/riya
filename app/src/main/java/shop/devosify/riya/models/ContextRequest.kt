package shop.devosify.riya.models

data class ContextRequest(
    val context: String,
    val user_id: String,
    val message: String
)