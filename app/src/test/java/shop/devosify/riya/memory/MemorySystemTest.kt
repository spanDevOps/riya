@RunWith(MockitoJUnitRunner::class)
class MemorySystemTest {
    @Mock private lateinit var memoryDao: MemoryDao
    @Mock private lateinit var gptRepository: GptRepository
    @Mock private lateinit var contextManager: ContextManager
    
    private lateinit var memorySystem: MemorySystem

    @Before
    fun setup() {
        memorySystem = MemorySystem(
            memoryDao = memoryDao,
            gptRepository = gptRepository,
            contextManager = contextManager
        )
    }

    @Test
    fun `test memory storage and retrieval`() = runTest {
        // Given
        val memory = MemoryEntity(
            type = MemoryType.EXPERIENCE,
            content = "Test memory",
            importance = 3,
            timestamp = System.currentTimeMillis()
        )
        whenever(memoryDao.insertMemory(any())).thenReturn(1L)
        whenever(memoryDao.getMemoryById(1L)).thenReturn(memory)

        // When
        val id = memorySystem.storeMemory(memory)
        val retrieved = memorySystem.getMemory(id)

        // Then
        assertThat(retrieved).isEqualTo(memory)
    }

    @Test
    fun `test memory importance calculation`() = runTest {
        // Given
        val content = "Important meeting with client"
        whenever(gptRepository.generateText(any()))
            .thenReturn(Result.success("""{"importance": 4}"""))

        // When
        val importance = memorySystem.calculateImportance(content)

        // Then
        assertThat(importance).isEqualTo(4)
    }

    @Test
    fun `test memory linking`() = runTest {
        // Given
        val memory1 = createTestMemory("Meeting discussion")
        val memory2 = createTestMemory("Meeting notes")
        whenever(memoryDao.findRelatedMemories(any())).thenReturn(listOf(memory2))

        // When
        val linked = memorySystem.findRelatedMemories(memory1)

        // Then
        assertThat(linked).hasSize(1)
        assertThat(linked.first()).isEqualTo(memory2)
    }
} 