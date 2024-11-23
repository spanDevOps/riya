@Singleton
class ShoppingPlatformAuthManager @Inject constructor(
    private val secureStorage: SecureStorage,
    private val webAuthenticator: WebAuthenticator,
    private val ttsService: TtsService,
    private val analyticsService: AnalyticsService
) {
    suspend fun initiateAuth(platform: ShoppingPlatform) {
        when (platform) {
            ShoppingPlatform.AMAZON -> {
                // Launch Amazon OAuth flow
                webAuthenticator.startOAuth(
                    url = AMAZON_AUTH_URL,
                    clientId = BuildConfig.AMAZON_CLIENT_ID,
                    scopes = listOf(
                        "profile",
                        "payments:widget",
                        "payments:shipping_address",
                        "amazon_pay_checkout",
                        "amazon_pay_automatic_payments"
                    )
                )
            }
            ShoppingPlatform.FLIPKART -> {
                // Launch Flipkart auth flow
                webAuthenticator.startOAuth(
                    url = FLIPKART_AUTH_URL,
                    clientId = BuildConfig.FLIPKART_CLIENT_ID,
                    scopes = listOf("profile", "orders", "cart")
                )
            }
        }
    }

    suspend fun handleAuthResponse(response: AuthResponse) {
        try {
            // Store tokens securely
            secureStorage.storeEncrypted(
                "${response.platform}_access_token",
                response.accessToken
            )
            secureStorage.storeEncrypted(
                "${response.platform}_refresh_token",
                response.refreshToken
            )

            // Notify user
            ttsService.speak(
                "Successfully connected your ${response.platform.name.lowercase()} account"
            )

            analyticsService.logEvent("shopping_auth_success", mapOf(
                "platform" to response.platform.name
            ))
        } catch (e: Exception) {
            analyticsService.logError("shopping_auth", e.message ?: "Unknown error")
            ttsService.speak("Sorry, there was a problem connecting your account")
        }
    }

    suspend fun refreshTokenIfNeeded(platform: ShoppingPlatform) {
        val accessToken = secureStorage.getEncrypted("${platform}_access_token")
        if (isTokenExpired(accessToken)) {
            val refreshToken = secureStorage.getEncrypted("${platform}_refresh_token")
            val newTokens = refreshAccessToken(platform, refreshToken)
            storeNewTokens(platform, newTokens)
        }
    }

    suspend fun isAuthorized(platform: ShoppingPlatform): Boolean {
        return secureStorage.getEncrypted("${platform}_access_token") != null
    }

    companion object {
        private const val AMAZON_AUTH_URL = "https://api.amazon.com/auth/o2/token"
        private const val FLIPKART_AUTH_URL = "https://api.flipkart.com/oauth/authorize"
    }
} 