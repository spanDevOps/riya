@Singleton
class CalendarIntegrationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val memoryRetrievalService: MemoryRetrievalService,
    private val gptRepository: GptRepository
) {
    private val contentResolver = context.contentResolver
    private val calendarUri = CalendarContract.Events.CONTENT_URI

    suspend fun getUpcomingEvents(days: Int = 7): Flow<List<CalendarEvent>> = flow {
        if (!hasCalendarPermission()) {
            throw SecurityException("Calendar permission not granted")
        }

        val now = System.currentTimeMillis()
        val end = now + (days * 24 * 60 * 60 * 1000)

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND " +
                "${CalendarContract.Events.DTSTART} <= ?"
        val selectionArgs = arrayOf(now.toString(), end.toString())

        contentResolver.query(
            calendarUri,
            EVENT_PROJECTION,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )?.use { cursor ->
            val events = mutableListOf<CalendarEvent>()
            while (cursor.moveToNext()) {
                events.add(cursor.toCalendarEvent())
            }
            emit(events)
        }
    }

    suspend fun suggestEventChanges(events: List<CalendarEvent>): List<EventSuggestion> {
        val memories = memoryRetrievalService.getRecentMemories()
        val prompt = """
            Analyze these calendar events and user memories to suggest optimizations:
            Events: ${events.joinToString("\n")}
            Memories: ${memories.joinToString("\n")}
            
            Suggest:
            1. Schedule conflicts
            2. Better timing based on preferences
            3. Related events to add
            4. Travel time considerations
            
            Format: JSON array of suggestions
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { response -> 
                gson.fromJson(response, Array<EventSuggestion>::class.java).toList()
            }
            .getOrDefault(emptyList())
    }

    private fun hasCalendarPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == 
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private val EVENT_PROJECTION = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.EVENT_LOCATION
        )
    }
}

data class CalendarEvent(
    val id: Long,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long,
    val location: String?
)

data class EventSuggestion(
    val type: SuggestionType,
    val description: String,
    val confidence: Float,
    val relatedEventId: Long?
)

enum class SuggestionType {
    CONFLICT,
    TIMING_OPTIMIZATION,
    NEW_EVENT,
    TRAVEL_TIME
} 