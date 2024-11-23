@Singleton
class KeyManager @Inject constructor(
    private val secureKeyStore: SecureKeyStore
) {
    private val keyCache = ConcurrentHashMap<String, String>()

    suspend fun getApiKey(keyType: KeyType): String {
        return keyCache.getOrPut(keyType.name) {
            secureKeyStore.getKey(keyType) ?: throw SecurityException("Key not found: ${keyType.name}")
        }
    }

    enum class KeyType {
        OPENAI_API,
        PORCUPINE_WAKE_WORD
    }
} 