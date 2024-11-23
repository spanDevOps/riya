@Singleton
class OrderTrackingSystem @Inject constructor(
    private val amazonService: AmazonService,
    private val flipkartService: FlipkartService,
    private val notificationManager: NotificationManager,
    private val ttsService: TtsService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val activeOrders = mutableMapOf<String, OrderTracker>()

    init {
        scope.launch {
            monitorOrders()
        }
    }

    suspend fun trackOrder(orderId: String, platform: ShoppingPlatform) {
        val tracker = OrderTracker(
            orderId = orderId,
            platform = platform,
            initialStatus = getOrderStatus(orderId, platform)
        )
        activeOrders[orderId] = tracker
    }

    private suspend fun monitorOrders() {
        while (true) {
            activeOrders.forEach { (orderId, tracker) ->
                val currentStatus = getOrderStatus(orderId, tracker.platform)
                if (currentStatus != tracker.lastStatus) {
                    handleStatusChange(orderId, tracker.lastStatus, currentStatus)
                    tracker.lastStatus = currentStatus
                }
            }
            delay(ORDER_CHECK_INTERVAL)
        }
    }

    private suspend fun getOrderStatus(
        orderId: String, 
        platform: ShoppingPlatform
    ): OrderStatus {
        return when (platform) {
            ShoppingPlatform.AMAZON -> amazonService.trackOrder(orderId)
            ShoppingPlatform.FLIPKART -> flipkartService.trackOrder(orderId)
        }
    }

    private suspend fun handleStatusChange(
        orderId: String,
        oldStatus: OrderStatus,
        newStatus: OrderStatus
    ) {
        val message = generateStatusMessage(orderId, newStatus)
        
        // Voice notification
        ttsService.speak(message)
        
        // Push notification
        notificationManager.showNotification(
            title = "Order Update",
            content = message,
            priority = NotificationPriority.HIGH
        )
    }
}

data class OrderTracker(
    val orderId: String,
    val platform: ShoppingPlatform,
    var lastStatus: OrderStatus
) 