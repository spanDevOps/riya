@Singleton
class WebAuthenticator @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun startOAuth(
        url: String,
        clientId: String,
        scopes: List<String>
    ) {
        val intent = CustomTabsIntent.Builder()
            .build()
            .apply {
                // Build OAuth URL
                val authUrl = buildAuthUrl(url, clientId, scopes)
                // Launch browser for authentication
                launchUrl(context, Uri.parse(authUrl))
            }
    }

    private fun buildAuthUrl(
        baseUrl: String,
        clientId: String,
        scopes: List<String>
    ): String {
        return Uri.parse(baseUrl)
            .buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", scopes.joinToString(" "))
            .appendQueryParameter("redirect_uri", BuildConfig.AUTH_REDIRECT_URI)
            .build()
            .toString()
    }
} 