@HiltAndroidTest
class MemorySystemIntegrationTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var memoryExtractionService: MemoryExtractionService
    @Inject lateinit var memoryRetrievalService: MemoryRetrievalService
    @Inject lateinit var crossModalMemoryService: CrossModalMemoryService
    @Inject lateinit var memoryDao: MemoryDao

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testMemoryExtractionAndRetrieval() = runBlocking {
        // 1. Extract memories from conversation
        val conversation = """
            User: I prefer working in the morning around 9 AM
            Assistant: I'll remember that you're most productive in the morning at 9 AM
            User: And I like my coffee with two sugars
            Assistant: Got it, you take your coffee with two sugars
        """.trimIndent()

        val extractionResult = memoryExtractionService.extractMemories(conversation)
        assertThat(extractionResult.isSuccess).isTrue()
        
        val memories = extractionResult.getOrNull()
        assertThat(memories).isNotNull()
        assertThat(memories).hasSize(2)

        // 2. Verify memory storage
        memories?.forEach { memory ->
            memoryDao.insertMemory(memory)
        }

        // 3. Test retrieval by context
        val relevantMemories = memoryRetrievalService.getRelevantMemories("What time do I work best?")
        assertThat(relevantMemories).isNotEmpty()
        assertThat(relevantMemories.first().content).contains("9 AM")

        // 4. Test cross-modal linking
        val visualMemory = VisualMemory(
            description = "Coffee cup on desk",
            objects = listOf("coffee cup", "desk"),
            emotions = emptyList(),
            timestamp = System.currentTimeMillis()
        )

        val coffeeMemory = memories?.find { it.content.contains("coffee") }
        assertThat(coffeeMemory).isNotNull()

        val linkedMemory = crossModalMemoryService.linkMemories(
            visualMemory,
            ConversationMemory(coffeeMemory!!.id, coffeeMemory.content)
        )

        assertThat(linkedMemory.relationshipType).isEqualTo(RelationType.SEMANTIC)
        assertThat(linkedMemory.confidence).isGreaterThan(0.7f)
    }

    @Test
    fun testMemoryPersistenceAndRetrieval() = runBlocking {
        // 1. Create test memories
        val testMemories = listOf(
            MemoryEntity(
                type = MemoryType.PREFERENCE,
                content = "Prefers dark mode in apps",
                importance = 4,
                tags = listOf("ui", "preferences"),
                timestamp = System.currentTimeMillis(),
                emotionalContext = "neutral"
            ),
            MemoryEntity(
                type = MemoryType.HABIT,
                content = "Checks email every morning at 8 AM",
                importance = 3,
                tags = listOf("routine", "email"),
                timestamp = System.currentTimeMillis(),
                emotionalContext = "focused"
            )
        )

        // 2. Store memories
        testMemories.forEach { memory ->
            memoryDao.insertMemory(memory)
        }

        // 3. Test different retrieval patterns
        // By type
        val preferences = memoryDao.getMemoriesByType(MemoryType.PREFERENCE)
        assertThat(preferences).hasSize(1)
        assertThat(preferences.first().content).contains("dark mode")

        // By tags
        val routineMemories = memoryDao.findMemoriesByTags(listOf("routine"))
        assertThat(routineMemories).hasSize(1)
        assertThat(routineMemories.first().content).contains("email")

        // By importance
        val importantMemories = memoryDao.getMemoriesByImportance(4)
        assertThat(importantMemories).hasSize(1)
        assertThat(importantMemories.first().type).isEqualTo(MemoryType.PREFERENCE)

        // 4. Test memory updates
        val updatedMemory = preferences.first().copy(
            importance = 5,
            emotionalContext = "positive"
        )
        memoryDao.updateMemory(updatedMemory)

        val retrievedMemory = memoryDao.getMemoryById(updatedMemory.id)
        assertThat(retrievedMemory?.importance).isEqualTo(5)
        assertThat(retrievedMemory?.emotionalContext).isEqualTo("positive")
    }
} 