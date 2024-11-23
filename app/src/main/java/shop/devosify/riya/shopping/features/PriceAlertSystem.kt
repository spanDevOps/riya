@Singleton
class PriceAlertSystem @Inject constructor(
    private val shoppingAssistantService: ShoppingAssistantService,
    private val notificationManager: NotificationManager
) {
    suspend fun watchPrice(product: Product, targetPrice: Double) {
        // Monitor price changes
        // Alert when price drops below target
    }
}

@Singleton
class DealFinder @Inject constructor(
    private val shoppingAssistantService: ShoppingAssistantService,
    private val userPreferencesManager: UserPreferencesManager
) {
    suspend fun findDeals() {
        // Find deals based on user preferences
        // Compare prices across platforms
        // Suggest best time to buy
    }
}

@Singleton
class SmartCartOptimizer @Inject constructor(
    private val shoppingAssistantService: ShoppingAssistantService,
    private val memoryDao: MemoryDao
) {
    suspend fun optimizeCart(cart: ShoppingCart) {
        // Suggest better combinations
        // Find bundle deals
        // Optimize for delivery costs
    }
}

@Singleton
class ReviewAnalyzer @Inject constructor(
    private val gptRepository: GptRepository
) {
    suspend fun analyzeReviews(product: Product) {
        // Analyze product reviews
        // Summarize key points
        // Identify potential issues
    }
} 