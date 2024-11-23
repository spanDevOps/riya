@Singleton
class SecureKeyManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val secureStorage: SecureStorage,
    private val analyticsService: AnalyticsService
) {
    private val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    private val cipher = Cipher.getInstance(SecurityConfig.ENCRYPTION_ALGORITHM)
    
    suspend fun generateMasterKey() {
        try {
            if (!keyStore.containsAlias(MASTER_KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    "AndroidKeyStore"
                )
                
                val keyGenParameterSpec = KeyGenParameterSpec.Builder(
                    MASTER_KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).apply {
                    setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    setKeySize(SecurityConfig.KEY_SIZE)
                    setUserAuthenticationRequired(false)
                }.build()

                keyGenerator.init(keyGenParameterSpec)
                keyGenerator.generateKey()
                
                analyticsService.logSecurityEvent("master_key_generated")
            }
        } catch (e: Exception) {
            analyticsService.logError("key_generation", e.message ?: "Unknown error")
            throw SecurityException("Failed to generate master key", e)
        }
    }

    suspend fun encryptData(data: ByteArray): EncryptedData {
        try {
            val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
            val iv = ByteArray(SecurityConfig.IV_LENGTH).apply {
                SecureRandom().nextBytes(this)
            }
            
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(
                SecurityConfig.AUTH_TAG_LENGTH, iv
            ))
            
            val encrypted = cipher.doFinal(data)
            return EncryptedData(encrypted, iv)
        } catch (e: Exception) {
            analyticsService.logError("data_encryption", e.message ?: "Unknown error")
            throw SecurityException("Failed to encrypt data", e)
        }
    }

    suspend fun decryptData(encryptedData: EncryptedData): ByteArray {
        try {
            val key = keyStore.getKey(MASTER_KEY_ALIAS, null) as SecretKey
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(
                SecurityConfig.AUTH_TAG_LENGTH, encryptedData.iv
            ))
            return cipher.doFinal(encryptedData.data)
        } catch (e: Exception) {
            analyticsService.logError("data_decryption", e.message ?: "Unknown error")
            throw SecurityException("Failed to decrypt data", e)
        }
    }

    suspend fun rotateKey() {
        try {
            // Generate new key
            generateMasterKey()
            
            // Re-encrypt all sensitive data with new key
            val sensitiveData = secureStorage.getAllSensitiveData()
            sensitiveData.forEach { (key, data) ->
                val decrypted = decryptData(data)
                val reEncrypted = encryptData(decrypted)
                secureStorage.storeSensitiveData(key, reEncrypted)
            }
            
            analyticsService.logSecurityEvent("key_rotation_completed")
        } catch (e: Exception) {
            analyticsService.logError("key_rotation", e.message ?: "Unknown error")
            throw SecurityException("Failed to rotate key", e)
        }
    }

    companion object {
        private const val MASTER_KEY_ALIAS = "riya_master_key"
    }
}

data class EncryptedData(
    val data: ByteArray,
    val iv: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as EncryptedData
        return data.contentEquals(other.data) && iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
} 