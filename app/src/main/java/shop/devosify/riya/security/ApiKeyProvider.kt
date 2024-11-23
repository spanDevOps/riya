@Singleton
class ApiKeyProvider @Inject constructor(
    private val secureStorage: SecureStorage
) {
    private var cachedKey: String? = null

    suspend fun getApiKey(): String {
        // Return cached key if available
        cachedKey?.let { return it }

        // Try to get from secure storage first
        return secureStorage.retrieveSensitiveData(API_KEY_ALIAS)?.toString(Charsets.UTF_8)
            ?: initializeApiKey()
    }

    private suspend fun initializeApiKey(): String {
        // Get key from BuildConfig (which gets it from local.properties during build)
        val key = BuildConfig.OPENAI_API_KEY

        // Store in secure storage for future use
        secureStorage.storeSensitiveData(
            API_KEY_ALIAS,
            key.toByteArray(Charsets.UTF_8)
        )

        // Cache the key
        cachedKey = key
        return key
    }

    companion object {
        private const val API_KEY_ALIAS = "openai_api_key"
    }
} 