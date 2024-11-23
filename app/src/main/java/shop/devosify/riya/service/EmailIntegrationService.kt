@Singleton
class EmailIntegrationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gptRepository: GptRepository,
    private val memoryRetrievalService: MemoryRetrievalService
) {
    private val contentResolver = context.contentResolver

    suspend fun getRecentEmails(count: Int = 10): Flow<List<EmailMessage>> = flow {
        if (!hasEmailPermission()) {
            throw SecurityException("Email permission not granted")
        }

        val uri = Uri.parse("content://com.android.email.provider/message")
        val projection = arrayOf(
            "_id",
            "address",
            "subject",
            "body",
            "timeStamp"
        )

        contentResolver.query(
            uri,
            projection,
            null,
            null,
            "timeStamp DESC LIMIT $count"
        )?.use { cursor ->
            val emails = mutableListOf<EmailMessage>()
            while (cursor.moveToNext()) {
                emails.add(cursor.toEmailMessage())
            }
            emit(emails)
        }
    }

    suspend fun suggestResponses(email: EmailMessage): List<EmailSuggestion> {
        val memories = memoryRetrievalService.getRelevantMemories(email.subject + " " + email.body)
        val prompt = """
            Analyze this email and user context to suggest responses:
            From: ${email.from}
            Subject: ${email.subject}
            Body: ${email.body}
            
            Context from memories:
            ${memories.joinToString("\n")}
            
            Suggest three different responses:
            1. Professional/Formal
            2. Friendly/Casual
            3. Brief/Concise
            
            Format: JSON array with:
            - type: FORMAL, CASUAL, BRIEF
            - subject: suggested subject
            - body: suggested response
            - context: why this response is appropriate
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response ->
                gson.fromJson(response, Array<EmailSuggestion>::class.java).toList()
            }
            .getOrDefault(emptyList())
    }

    private fun hasEmailPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_EMAIL) == 
            PackageManager.PERMISSION_GRANTED
    }
}

data class EmailMessage(
    val id: Long,
    val from: String,
    val subject: String,
    val body: String,
    val timestamp: Long,
    val isRead: Boolean,
    val hasAttachments: Boolean
)

data class EmailSuggestion(
    val type: ResponseType,
    val subject: String,
    val body: String,
    val context: String
)

enum class ResponseType {
    FORMAL,
    CASUAL,
    BRIEF
} 