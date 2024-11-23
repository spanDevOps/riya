@HiltAndroidTest
class PerformanceTest {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Inject lateinit var voiceCommandProcessor: VoiceCommandProcessor
    @Inject lateinit var memorySystem: MemorySystem
    @Inject lateinit var serviceCoordinator: ServiceCoordinator

    @Test
    fun testCommandProcessingPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                val result = voiceCommandProcessor.processCommand("what's the time")
                    .first()
                assertThat(result).isInstanceOf(CommandResult.Success::class.java)
            }
        }
    }

    @Test
    fun testMemorySystemPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Insert test memories
                repeat(100) {
                    memorySystem.storeMemory(createTestMemory(it))
                }

                // Query memories
                val memories = memorySystem.searchMemories("test")
                assertThat(memories).hasSize(100)
            }
        }
    }

    @Test
    fun testServiceCoordinationPerformance() {
        benchmarkRule.measureRepeated {
            runBlocking {
                // Simulate multiple service updates
                repeat(10) {
                    serviceCoordinator.updateContext(createTestContext())
                }
            }
        }
    }
} 