@Singleton
class FlipkartService @Inject constructor(
    private val flipkartApi: FlipkartApi,
    private val authManager: AuthManager,
    private val analyticsService: AnalyticsService
) {
    suspend fun searchProducts(query: String, filters: Map<String, Any>): List<Product> {
        return try {
            val searchParams = buildSearchParams(query, filters)
            flipkartApi.searchProducts(searchParams)
        } catch (e: Exception) {
            analyticsService.logError("flipkart_search", e.message ?: "Unknown error")
            emptyList()
        }
    }

    suspend fun repeatOrder(previousOrder: OrderDetails): OrderResult {
        return try {
            // Verify cart
            val cart = flipkartApi.createCart()
            previousOrder.items.forEach { item ->
                cart.addItem(item)
            }

            // Place order
            val order = flipkartApi.placeOrder(
                cart = cart,
                address = previousOrder.address,
                paymentMethod = previousOrder.paymentMethod
            )

            OrderResult.Success(order.orderId)
        } catch (e: Exception) {
            analyticsService.logError("flipkart_order", e.message ?: "Unknown error")
            OrderResult.Error(e.message ?: "Failed to place order")
        }
    }

    suspend fun trackOrder(orderId: String): OrderStatus {
        return try {
            flipkartApi.getOrderStatus(orderId)
        } catch (e: Exception) {
            analyticsService.logError("flipkart_tracking", e.message ?: "Unknown error")
            OrderStatus.Unknown
        }
    }
} 