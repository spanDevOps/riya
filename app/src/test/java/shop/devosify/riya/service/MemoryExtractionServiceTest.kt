@RunWith(MockitoJUnitRunner::class)
class MemoryExtractionServiceTest : BaseRiyaTest() {
    @Mock
    lateinit var gptRepository: GptRepository
    
    @Mock
    lateinit var memoryDao: MemoryDao

    private lateinit var memoryExtractionService: MemoryExtractionService

    override fun setupMocks() {
        memoryExtractionService = MemoryExtractionService(gptRepository, memoryDao)
    }

    override fun clearMocks() {
        clearAllMocks()
    }

    @Test
    fun `test memory extraction from conversation`() = runBlockingTest {
        // Given
        val conversation = """
            User: I prefer my coffee with two sugars
            Assistant: I'll remember that you like your coffee sweet with two sugars
        """.trimIndent()

        val expectedMemory = MemoryEntity(
            type = "PREFERENCE",
            content = "User prefers coffee with two sugars",
            importance = 4,
            context = "Beverage preferences",
            timestamp = System.currentTimeMillis()
        )

        whenever(gptRepository.generateText(any()))
            .thenReturn(Result.success(gson.toJson(listOf(expectedMemory))))

        // When
        val result = memoryExtractionService.extractMemories(conversation)

        // Then
        assertThat(result.isSuccess).isTrue()
        val memories = result.getOrNull()
        assertThat(memories).isNotNull()
        assertThat(memories!!.first().type).isEqualTo("PREFERENCE")
        assertThat(memories.first().content).contains("two sugars")
    }
} 