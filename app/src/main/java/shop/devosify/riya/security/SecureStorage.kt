@Singleton
class SecureStorage @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureKeyManager: SecureKeyManager,
    private val analyticsService: AnalyticsService
) {
    private val encryptedPrefs by lazy {
        createEncryptedPrefs()
    }

    private fun createEncryptedPrefs(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        
        return EncryptedSharedPreferences.create(
            "secure_storage",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    suspend fun storeSensitiveData(key: String, data: ByteArray) {
        try {
            val encryptedData = secureKeyManager.encryptData(data)
            val encodedData = Base64.encodeToString(encryptedData.data, Base64.DEFAULT)
            val encodedIv = Base64.encodeToString(encryptedData.iv, Base64.DEFAULT)
            
            encryptedPrefs.edit()
                .putString("${key}_data", encodedData)
                .putString("${key}_iv", encodedIv)
                .apply()
            
            analyticsService.logSecurityEvent("data_stored")
        } catch (e: Exception) {
            analyticsService.logError("data_storage", e.message ?: "Unknown error")
            throw SecurityException("Failed to store sensitive data", e)
        }
    }

    suspend fun retrieveSensitiveData(key: String): ByteArray? {
        return try {
            val encodedData = encryptedPrefs.getString("${key}_data", null)
            val encodedIv = encryptedPrefs.getString("${key}_iv", null)
            
            if (encodedData != null && encodedIv != null) {
                val encryptedData = EncryptedData(
                    data = Base64.decode(encodedData, Base64.DEFAULT),
                    iv = Base64.decode(encodedIv, Base64.DEFAULT)
                )
                secureKeyManager.decryptData(encryptedData)
            } else {
                null
            }
        } catch (e: Exception) {
            analyticsService.logError("data_retrieval", e.message ?: "Unknown error")
            throw SecurityException("Failed to retrieve sensitive data", e)
        }
    }

    suspend fun getAllSensitiveData(): Map<String, EncryptedData> {
        return try {
            encryptedPrefs.all.mapNotNull { (key, _) ->
                if (key.endsWith("_data")) {
                    val baseKey = key.removeSuffix("_data")
                    val encodedData = encryptedPrefs.getString(key, null)
                    val encodedIv = encryptedPrefs.getString("${baseKey}_iv", null)
                    
                    if (encodedData != null && encodedIv != null) {
                        baseKey to EncryptedData(
                            data = Base64.decode(encodedData, Base64.DEFAULT),
                            iv = Base64.decode(encodedIv, Base64.DEFAULT)
                        )
                    } else null
                } else null
            }.toMap()
        } catch (e: Exception) {
            analyticsService.logError("data_retrieval_all", e.message ?: "Unknown error")
            throw SecurityException("Failed to retrieve all sensitive data", e)
        }
    }

    suspend fun removeSensitiveData(key: String) {
        try {
            encryptedPrefs.edit()
                .remove("${key}_data")
                .remove("${key}_iv")
                .apply()
            
            analyticsService.logSecurityEvent("data_removed")
        } catch (e: Exception) {
            analyticsService.logError("data_removal", e.message ?: "Unknown error")
            throw SecurityException("Failed to remove sensitive data", e)
        }
    }

    suspend fun clearAllData() {
        try {
            encryptedPrefs.edit().clear().apply()
            analyticsService.logSecurityEvent("data_cleared")
        } catch (e: Exception) {
            analyticsService.logError("data_clear", e.message ?: "Unknown error")
            throw SecurityException("Failed to clear all data", e)
        }
    }

    fun getDatabaseKey(): ByteArray {
        val key = encryptedPrefs.getString(DATABASE_KEY, null)
        return if (key != null) {
            Base64.decode(key, Base64.DEFAULT)
        } else {
            // Generate and store new key if not exists
            val newKey = ByteArray(32).apply {
                SecureRandom().nextBytes(this)
            }
            encryptedPrefs.edit()
                .putString(DATABASE_KEY, Base64.encodeToString(newKey, Base64.DEFAULT))
                .apply()
            newKey
        }
    }

    companion object {
        private const val DATABASE_KEY = "database_encryption_key"
    }
} 