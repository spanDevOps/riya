@HiltAndroidTest
class ShoppingIntegrationTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var shoppingAssistantService: ShoppingAssistantService
    @Inject lateinit var orderTrackingSystem: OrderTrackingSystem
    @Inject lateinit var shoppingPreferencesManager: ShoppingPreferencesManager

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testCompleteShoppingFlow() = runTest {
        // Test shopping request
        val result = shoppingAssistantService.processShoppingRequest(
            "find me a gaming laptop under 80000"
        )
        assertThat(result).isInstanceOf(ShoppingResult.ProductSuggestions::class.java)

        // Verify preferences are considered
        val suggestions = (result as ShoppingResult.ProductSuggestions).products
        val preferences = shoppingPreferencesManager.getPreferences()
        suggestions.forEach { product ->
            assertThat(product.price.amount)
                .isLessThan(80000.0)
        }

        // Test order tracking
        val orderId = "test_order_123"
        orderTrackingSystem.trackOrder(orderId, ShoppingPlatform.AMAZON)
        val status = orderTrackingSystem.getOrderStatus(orderId)
        assertThat(status).isNotNull()
    }

    @Test
    fun testPriceAlerts() = runTest {
        // Setup price alert
        val product = createTestProduct()
        val targetPrice = 75000.0
        shoppingAssistantService.watchPrice(product, targetPrice)

        // Simulate price drop
        simulatePriceDrop(product, 70000.0)

        // Verify alert
        val notifications = getNotifications()
        assertThat(notifications).anyMatch { 
            it.contains("Price dropped") && it.contains(product.name)
        }
    }
} 