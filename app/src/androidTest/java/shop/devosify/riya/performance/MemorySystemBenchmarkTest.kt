@HiltAndroidTest
class MemorySystemBenchmarkTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val benchmarkRule = BenchmarkRule()

    @Inject lateinit var memoryDao: MemoryDao
    @Inject lateinit var memoryRetrievalService: MemoryRetrievalService
    @Inject lateinit var patternAnalysisService: PatternAnalysisService
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var crossModalMemoryService: CrossModalMemoryService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testMemoryInsertionPerformance() {
        // Test bulk memory insertion
        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("memory_insertion")
            
            runBlocking {
                val memories = createTestMemories(1000) // Test with 1000 memories
                memories.forEach { memory ->
                    memoryDao.insertMemory(memory)
                }
            }
            
            val metrics = performanceMonitor.endOperation("memory_insertion")
            
            // Verify performance
            assertThat(metrics.duration).isLessThan(5000) // Should complete within 5 seconds
            assertThat(metrics.memoryUsage).isLessThan(50 * 1024 * 1024) // Less than 50MB
        }
    }

    @Test
    fun testMemoryRetrievalPerformance() {
        runBlocking {
            // Setup test data
            val memories = createTestMemories(1000)
            memories.forEach { memoryDao.insertMemory(it) }
        }

        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("memory_retrieval")
            
            runBlocking {
                val query = "test query with specific context"
                val relevantMemories = memoryRetrievalService.getRelevantMemories(query, limit = 10)
                
                val metrics = performanceMonitor.endOperation("memory_retrieval")
                
                // Verify retrieval performance
                assertThat(metrics.duration).isLessThan(1000) // Should retrieve within 1 second
                assertThat(metrics.cpuUsage).isLessThan(30) // Less than 30% CPU usage
                assertThat(relevantMemories).isNotEmpty()
            }
        }
    }

    @Test
    fun testPatternAnalysisPerformance() {
        runBlocking {
            // Setup test data with various memory types
            val memories = createDiverseMemories(500)
            memories.forEach { memoryDao.insertMemory(it) }
        }

        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("pattern_analysis")
            
            runBlocking {
                val patterns = patternAnalysisService.getUserPatterns().first()
                
                val metrics = performanceMonitor.endOperation("pattern_analysis")
                
                // Verify analysis performance
                assertThat(metrics.duration).isLessThan(3000) // Should analyze within 3 seconds
                assertThat(metrics.cpuUsage).isLessThan(50) // Less than 50% CPU usage
                assertThat(patterns).isNotEmpty()
            }
        }
    }

    @Test
    fun testCrossModalMemoryLinking() {
        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("cross_modal_linking")
            
            runBlocking {
                val visualMemory = createTestVisualMemory()
                val conversationMemory = createTestConversationMemory()
                
                crossModalMemoryService.linkMemories(visualMemory, conversationMemory)
                
                val metrics = performanceMonitor.endOperation("cross_modal_linking")
                
                // Verify linking performance
                assertThat(metrics.duration).isLessThan(500) // Should link within 500ms
                assertThat(metrics.memoryUsage).isLessThan(10 * 1024 * 1024) // Less than 10MB
            }
        }
    }

    @Test
    fun testMemorySystemScaling() {
        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("system_scaling")
            
            runBlocking {
                // Test with increasing memory loads
                val memoryCounts = listOf(100, 1000, 10000)
                
                memoryCounts.forEach { count ->
                    val memories = createTestMemories(count)
                    val insertStart = System.nanoTime()
                    
                    memories.forEach { memoryDao.insertMemory(it) }
                    
                    val insertDuration = (System.nanoTime() - insertStart) / 1_000_000 // ms
                    
                    // Verify scaling performance
                    assertThat(insertDuration).isLessThan(count.toLong()) // Should scale linearly
                }
            }
            
            val metrics = performanceMonitor.endOperation("system_scaling")
            assertThat(metrics.success).isTrue()
        }
    }

    private fun createTestMemories(count: Int): List<MemoryEntity> {
        return List(count) { index ->
            MemoryEntity(
                type = MemoryType.EXPERIENCE,
                content = "Test memory $index with specific context",
                importance = (index % 5) + 1,
                timestamp = System.currentTimeMillis() - (index * 1000),
                tags = listOf("test", "benchmark", "memory_$index")
            )
        }
    }

    private fun createDiverseMemories(count: Int): List<MemoryEntity> {
        val types = listOf(
            MemoryType.EXPERIENCE,
            MemoryType.FACT,
            MemoryType.PREFERENCE,
            MemoryType.ROUTINE
        )
        
        return List(count) { index ->
            MemoryEntity(
                type = types[index % types.size],
                content = "Diverse memory $index with type ${types[index % types.size]}",
                importance = (index % 5) + 1,
                timestamp = System.currentTimeMillis() - (index * 1000),
                tags = listOf("diverse", types[index % types.size].name.lowercase())
            )
        }
    }

    private fun createTestVisualMemory(): VisualMemory {
        return VisualMemory(
            description = "Test visual scene",
            objects = listOf("object1", "object2"),
            emotions = listOf("neutral", "happy"),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun createTestConversationMemory(): ConversationMemory {
        return ConversationMemory(
            content = "Test conversation about the visual scene",
            sentiment = "positive",
            timestamp = System.currentTimeMillis()
        )
    }
} 