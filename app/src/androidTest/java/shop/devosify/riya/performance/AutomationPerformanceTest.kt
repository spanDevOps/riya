@HiltAndroidTest
class AutomationPerformanceTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val benchmarkRule = BenchmarkRule()

    @Inject lateinit var locationAutomationService: LocationAutomationService
    @Inject lateinit var placesService: PlacesService
    @Inject lateinit var geofencingService: GeofencingService
    @Inject lateinit var performanceMonitor: PerformanceMonitor
    @Inject lateinit var systemMonitorService: SystemMonitorService

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testGeofenceSetupPerformance() = runTest {
        // Create test locations
        val locations = createTestLocations(100) // Test with 100 locations
        
        benchmarkRule.measureRepeated {
            // Measure geofence setup time
            performanceMonitor.startOperation("geofence_setup")
            
            runBlocking {
                locations.forEach { location ->
                    placesService.addLocation(location)
                }
                geofencingService.setupGeofencesForFrequentPlaces()
            }
            
            val metrics = performanceMonitor.endOperation("geofence_setup")
            
            // Verify performance
            assertThat(metrics.duration).isLessThan(5000) // Should take less than 5 seconds
            assertThat(metrics.memoryUsage).isLessThan(50 * 1024 * 1024) // Less than 50MB
        }
    }

    @Test
    fun testAutomationTriggerLatency() = runTest {
        // Setup test automation rules
        val rules = createTestRules(50) // Test with 50 rules
        rules.forEach { locationAutomationService.addRule(it) }

        benchmarkRule.measureRepeated {
            // Measure trigger processing time
            performanceMonitor.startOperation("automation_trigger")
            
            runBlocking {
                val location = createTestLocation()
                locationAutomationService.handleLocationTrigger(
                    location = location,
                    trigger = TriggerType.ENTER
                )
            }
            
            val metrics = performanceMonitor.endOperation("automation_trigger")
            
            // Verify latency
            assertThat(metrics.duration).isLessThan(1000) // Should process within 1 second
            assertThat(metrics.cpuUsage).isLessThan(30) // Less than 30% CPU usage
        }
    }

    @Test
    fun testConcurrentAutomationProcessing() = runTest {
        // Create multiple automation triggers
        val locations = createTestLocations(10)
        val triggers = listOf(
            TriggerType.ENTER,
            TriggerType.EXIT,
            TriggerType.DWELL
        )

        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("concurrent_processing")
            
            runBlocking {
                val jobs = locations.flatMap { location ->
                    triggers.map { trigger ->
                        async {
                            locationAutomationService.handleLocationTrigger(
                                location = location,
                                trigger = trigger
                            )
                        }
                    }
                }
                jobs.awaitAll()
            }
            
            val metrics = performanceMonitor.endOperation("concurrent_processing")
            
            // Verify concurrent performance
            assertThat(metrics.duration).isLessThan(3000) // Should handle all within 3 seconds
            assertThat(metrics.memoryUsage).isLessThan(100 * 1024 * 1024) // Less than 100MB
        }
    }

    @Test
    fun testPatternAnalysisPerformance() = runTest {
        // Generate test data
        val memories = createTestMemories(1000) // Test with 1000 memories
        runBlocking {
            memories.forEach { memoryDao.insertMemory(it) }
        }

        benchmarkRule.measureRepeated {
            performanceMonitor.startOperation("pattern_analysis")
            
            runBlocking {
                val patterns = patternAnalysisService.getUserPatterns().first()
                
                // Verify analysis performance
                assertThat(patterns).isNotEmpty()
                val metrics = performanceMonitor.endOperation("pattern_analysis")
                
                assertThat(metrics.duration).isLessThan(10000) // Should analyze within 10 seconds
                assertThat(metrics.cpuUsage).isLessThan(50) // Less than 50% CPU usage
            }
        }
    }

    @Test
    fun testSystemResourceUsage() = runTest {
        // Monitor system resources during automation
        val initialState = systemMonitorService.getCurrentState()
        
        benchmarkRule.measureRepeated {
            runBlocking {
                // Run intensive automation tasks
                repeat(10) {
                    val location = createTestLocation()
                    locationAutomationService.handleLocationTrigger(
                        location = location,
                        trigger = TriggerType.ENTER
                    )
                    delay(100) // Simulate real-world timing
                }
            }
        }

        val finalState = systemMonitorService.getCurrentState()
        
        // Verify resource usage
        val memoryUsage = finalState.memoryUsage - initialState.memoryUsage
        val batteryDrain = initialState.batteryLevel - finalState.batteryLevel
        
        assertThat(memoryUsage).isLessThan(200 * 1024 * 1024) // Less than 200MB increase
        assertThat(batteryDrain).isLessThan(2) // Less than 2% battery drain
    }

    private fun createTestLocations(count: Int): List<LocationData> {
        return List(count) { index ->
            LocationData(
                id = "test_location_$index",
                name = "Test Location $index",
                type = GeofenceType.FREQUENT,
                latitude = 37.4220 + (index * 0.001),
                longitude = -122.0841 + (index * 0.001)
            )
        }
    }

    private fun createTestRules(count: Int): List<AutomationRule> {
        return List(count) { index ->
            AutomationRule(
                id = UUID.randomUUID().toString(),
                name = "Test Rule $index",
                type = PatternType.ROUTINE,
                condition = AutomationCondition(
                    location = GeofenceType.HOME,
                    trigger = TriggerType.ENTER
                ),
                action = AutomationAction(
                    type = ActionType.MODIFY_SETTING,
                    parameters = mapOf("setting_$index" to "value_$index")
                ),
                confidence = 0.9f
            )
        }
    }

    private fun createTestMemories(count: Int): List<MemoryEntity> {
        return List(count) { index ->
            MemoryEntity(
                type = MemoryType.EXPERIENCE,
                content = "Test memory $index",
                importance = (index % 5) + 1,
                timestamp = System.currentTimeMillis() - (index * 1000),
                tags = listOf("test", "automation", "memory_$index")
            )
        }
    }
} 