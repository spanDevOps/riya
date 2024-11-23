@Singleton
class AmazonService @Inject constructor(
    private val amazonApi: AmazonApi,
    private val authManager: AuthManager,
    private val analyticsService: AnalyticsService
) {
    suspend fun searchProducts(query: String, filters: Map<String, Any>): List<Product> {
        return try {
            val searchParams = buildSearchParams(query, filters)
            amazonApi.searchProducts(searchParams)
        } catch (e: Exception) {
            analyticsService.logError("amazon_search", e.message ?: "Unknown error")
            emptyList()
        }
    }

    suspend fun repeatOrder(previousOrder: OrderDetails): OrderResult {
        return try {
            // Check if all items are still available
            val availability = checkAvailability(previousOrder.items)
            if (!availability.allAvailable) {
                return OrderResult.Error(
                    "Some items are no longer available: ${availability.unavailableItems}"
                )
            }

            // Place order
            val order = amazonApi.placeOrder(
                items = previousOrder.items,
                address = previousOrder.address,
                paymentMethod = previousOrder.paymentMethod
            )

            OrderResult.Success(order.orderId)
        } catch (e: Exception) {
            analyticsService.logError("amazon_order", e.message ?: "Unknown error")
            OrderResult.Error(e.message ?: "Failed to place order")
        }
    }

    suspend fun trackOrder(orderId: String): OrderStatus {
        return try {
            amazonApi.getOrderStatus(orderId)
        } catch (e: Exception) {
            analyticsService.logError("amazon_tracking", e.message ?: "Unknown error")
            OrderStatus.Unknown
        }
    }
} 