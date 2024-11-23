@Singleton
class ShoppingAssistantService @Inject constructor(
    private val amazonService: AmazonService,
    private val flipkartService: FlipkartService,
    private val userPreferencesManager: UserPreferencesManager,
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository,
    private val ttsService: TtsService
) {
    suspend fun processShoppingRequest(request: String): ShoppingResult {
        // Use GPT to understand shopping intent
        val shoppingIntent = analyzeShoppingIntent(request)
        
        return when (shoppingIntent.type) {
            ShoppingIntentType.REPEAT_ORDER -> handleRepeatOrder(shoppingIntent)
            ShoppingIntentType.NEW_PRODUCT -> findAndSuggestProducts(shoppingIntent)
            ShoppingIntentType.PRICE_CHECK -> checkPrices(shoppingIntent)
            ShoppingIntentType.AVAILABILITY -> checkAvailability(shoppingIntent)
        }
    }

    private suspend fun analyzeShoppingIntent(request: String): ShoppingIntent {
        val prompt = """
            Analyze this shopping request:
            "$request"
            
            Consider:
            1. Is it a repeat order?
            2. Is it a new product search?
            3. Is it a price check?
            4. Is it an availability check?
            
            Return JSON with:
            {
                "type": "REPEAT_ORDER|NEW_PRODUCT|PRICE_CHECK|AVAILABILITY",
                "product": {
                    "name": string,
                    "category": string,
                    "specifications": [string],
                    "priceRange": {
                        "min": number,
                        "max": number
                    }
                },
                "urgency": "LOW|MEDIUM|HIGH",
                "preferences": [string]
            }
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { gson.fromJson(it, ShoppingIntent::class.java) }
            .getOrThrow()
    }

    private suspend fun handleRepeatOrder(intent: ShoppingIntent): ShoppingResult {
        // Find previous order from memory
        val previousOrder = memoryDao.findPreviousOrder(intent.product.name)
            ?: return ShoppingResult.Error("No previous order found for ${intent.product.name}")

        // Get preferred platform
        val platform = userPreferencesManager.getPreferredShoppingPlatform()

        // Place order
        val result = when (platform) {
            ShoppingPlatform.AMAZON -> amazonService.repeatOrder(previousOrder)
            ShoppingPlatform.FLIPKART -> flipkartService.repeatOrder(previousOrder)
        }

        // Store in memory
        if (result is OrderResult.Success) {
            memoryDao.insertMemory(MemoryEntity(
                type = MemoryType.SHOPPING,
                content = "Repeated order for ${intent.product.name}",
                importance = 3,
                metadata = mapOf(
                    "orderId" to result.orderId,
                    "platform" to platform.name,
                    "product" to intent.product.name
                )
            ))
        }

        return when (result) {
            is OrderResult.Success -> ShoppingResult.Success(
                "Order placed successfully. Order ID: ${result.orderId}"
            )
            is OrderResult.Error -> ShoppingResult.Error(result.message)
        }
    }

    private suspend fun findAndSuggestProducts(intent: ShoppingIntent): ShoppingResult {
        // Get preferred platform
        val platform = userPreferencesManager.getPreferredShoppingPlatform()
        
        // Search for products
        val products = when (platform) {
            ShoppingPlatform.AMAZON -> amazonService.searchProducts(
                query = intent.product.name,
                filters = buildFilters(intent)
            )
            ShoppingPlatform.FLIPKART -> flipkartService.searchProducts(
                query = intent.product.name,
                filters = buildFilters(intent)
            )
        }

        // Filter and rank products
        val rankedProducts = rankProducts(products, intent)

        // Present top options
        return if (rankedProducts.isNotEmpty()) {
            presentProductOptions(rankedProducts)
            ShoppingResult.ProductSuggestions(rankedProducts)
        } else {
            ShoppingResult.Error("No products found matching your criteria")
        }
    }

    private suspend fun presentProductOptions(products: List<Product>) {
        val topProducts = products.take(3)
        val message = buildString {
            append("I found these options:\n")
            topProducts.forEachIndexed { index, product ->
                append("${index + 1}. ${product.name} - ${product.price}\n")
                append("   Rating: ${product.rating} stars\n")
            }
            append("\nWould you like to know more about any of these options?")
        }
        ttsService.speak(message)
    }

    private suspend fun rankProducts(
        products: List<Product>,
        intent: ShoppingIntent
    ): List<Product> {
        // Get user preferences and history
        val preferences = userPreferencesManager.getShoppingPreferences()
        val purchaseHistory = memoryDao.getPurchaseHistory()

        return products
            .filter { it.price.inRange(intent.product.priceRange) }
            .sortedWith(compareBy(
                // Higher rating
                { -it.rating },
                // Matches preferences
                { -preferences.matchScore(it) },
                // Price within range
                { intent.product.priceRange.fitScore(it.price) }
            ))
    }
}

sealed class ShoppingResult {
    data class Success(val message: String) : ShoppingResult()
    data class Error(val message: String) : ShoppingResult()
    data class ProductSuggestions(val products: List<Product>) : ShoppingResult()
}

data class ShoppingIntent(
    val type: ShoppingIntentType,
    val product: ProductRequest,
    val urgency: Urgency,
    val preferences: List<String>
)

data class ProductRequest(
    val name: String,
    val category: String,
    val specifications: List<String>,
    val priceRange: PriceRange
)

enum class ShoppingIntentType {
    REPEAT_ORDER,
    NEW_PRODUCT,
    PRICE_CHECK,
    AVAILABILITY
}

enum class ShoppingPlatform {
    AMAZON,
    FLIPKART
} 