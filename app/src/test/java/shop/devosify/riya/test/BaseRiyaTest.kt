abstract class BaseRiyaTest {
    @get:Rule
    val testRule = RiyaTestRule()

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    protected val testDispatcher = TestCoroutineDispatcher()
    protected val testScope = TestCoroutineScope(testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        setupMocks()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        testScope.cleanupTestCoroutines()
        clearMocks()
    }

    abstract fun setupMocks()
    abstract fun clearMocks()

    protected fun runBlockingTest(block: suspend TestCoroutineScope.() -> Unit) =
        testScope.runBlockingTest(block)
} 