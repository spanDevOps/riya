@RunWith(MockitoJUnitRunner::class)
class ContextManagerTest {
    @Mock private lateinit var memorySystem: MemorySystem
    @Mock private lateinit var locationService: LocationService
    @Mock private lateinit var systemMonitorService: SystemMonitorService
    
    private lateinit var contextManager: ContextManager

    @Before
    fun setup() {
        contextManager = ContextManager(
            memorySystem = memorySystem,
            locationService = locationService,
            systemMonitorService = systemMonitorService
        )
    }

    @Test
    fun `test context building`() = runTest {
        // Given
        val location = Location("test").apply {
            latitude = 37.4219983
            longitude = -122.084
        }
        whenever(locationService.getCurrentLocation()).thenReturn(location)
        whenever(systemMonitorService.getBatteryLevel()).thenReturn(85)

        // When
        val context = contextManager.buildContext()

        // Then
        assertThat(context.location).isEqualTo(location)
        assertThat(context.systemState.batteryLevel).isEqualTo(85)
    }

    @Test
    fun `test context update triggers`() = runTest {
        // Given
        val contextCollector = mutableListOf<RiyaContext>()
        val job = launch {
            contextManager.contextFlow.collect { contextCollector.add(it) }
        }

        // When
        contextManager.updateLocation(Location("test"))
        contextManager.updateSystemState(SystemState(batteryLevel = 90))

        // Then
        assertThat(contextCollector).hasSize(2)
        job.cancel()
    }

    @Test
    fun `test context persistence`() = runTest {
        // Given
        val context = createTestContext()
        
        // When
        contextManager.saveContext(context)
        val loaded = contextManager.loadLastContext()

        // Then
        assertThat(loaded).isEqualTo(context)
    }
} 