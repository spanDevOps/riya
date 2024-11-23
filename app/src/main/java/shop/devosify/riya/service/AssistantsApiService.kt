package shop.devosify.riya.service

import retrofit2.Response
import retrofit2.http.*

interface AssistantsApiService {
    @POST("v1/assistants")
    suspend fun createAssistant(
        @Body request: CreateAssistantRequest
    ): Response<Assistant>

    @POST("v1/threads")
    suspend fun createThread(): Response<Thread>

    @POST("v1/threads/{threadId}/messages")
    suspend fun addMessage(
        @Path("threadId") threadId: String,
        @Body request: AddMessageRequest
    ): Response<Message>

    @POST("v1/threads/{threadId}/runs")
    suspend fun createRun(
        @Path("threadId") threadId: String,
        @Body request: CreateRunRequest
    ): Response<Run>

    @GET("v1/threads/{threadId}/runs/{runId}")
    suspend fun retrieveRun(
        @Path("threadId") threadId: String,
        @Path("runId") runId: String
    ): Response<Run>
}

data class Assistant(
    val id: String,
    val name: String,
    val instructions: String
)

data class Thread(
    val id: String
)

data class Message(
    val id: String,
    val content: List<MessageContent>
)

data class MessageContent(
    val type: String,
    val text: TextContent
)

data class TextContent(
    val value: String
)

data class CreateAssistantRequest(
    val model: String = "gpt-4-1106-preview",
    val name: String,
    val instructions: String
)

data class AddMessageRequest(
    val role: String = "user",
    val content: String
)

data class CreateRunRequest(
    val assistant_id: String
)

data class Run(
    val id: String,
    val status: String
)
