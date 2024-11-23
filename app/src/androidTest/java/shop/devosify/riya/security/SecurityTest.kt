@HiltAndroidTest
class SecurityTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @Inject lateinit var secureKeyManager: SecureKeyManager
    @Inject lateinit var secureStorage: SecureStorage
    @Inject lateinit var apiKeyProvider: ApiKeyProvider

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun testKeyGeneration() = runTest {
        // Generate master key
        secureKeyManager.generateMasterKey()
        
        // Verify key exists
        val keyStore = KeyStore.getInstance("AndroidKeyStore")
        keyStore.load(null)
        assertThat(keyStore.containsAlias("riya_master_key")).isTrue()
    }

    @Test
    fun testDataEncryption() = runTest {
        val testData = "sensitive_data".toByteArray()
        
        // Encrypt data
        val encryptedData = secureKeyManager.encryptData(testData)
        assertThat(encryptedData.data).isNotEqualTo(testData)
        
        // Decrypt data
        val decryptedData = secureKeyManager.decryptData(encryptedData)
        assertThat(decryptedData).isEqualTo(testData)
    }

    @Test
    fun testKeyRotation() = runTest {
        // Store initial data
        val testKey = "test_key"
        val testData = "test_data".toByteArray()
        secureStorage.storeSensitiveData(testKey, testData)
        
        // Rotate key
        secureKeyManager.rotateKey()
        
        // Verify data is still accessible
        val retrievedData = secureStorage.retrieveSensitiveData(testKey)
        assertThat(retrievedData).isEqualTo(testData)
    }

    @Test
    fun testApiKeyManagement() = runTest {
        // Get initial API key
        val initialKey = apiKeyProvider.getApiKey()
        assertThat(initialKey).isNotEmpty()
        
        // Rotate key
        apiKeyProvider.rotateKey()
        val newKey = apiKeyProvider.getApiKey()
        
        // Verify key was rotated
        assertThat(newKey).isNotEqualTo(initialKey)
    }

    @Test
    fun testSecureStorageIsolation() = runTest {
        val key1 = "key1"
        val key2 = "key2"
        val data1 = "data1".toByteArray()
        val data2 = "data2".toByteArray()
        
        // Store data
        secureStorage.storeSensitiveData(key1, data1)
        secureStorage.storeSensitiveData(key2, data2)
        
        // Verify data isolation
        val retrieved1 = secureStorage.retrieveSensitiveData(key1)
        val retrieved2 = secureStorage.retrieveSensitiveData(key2)
        
        assertThat(retrieved1).isEqualTo(data1)
        assertThat(retrieved2).isEqualTo(data2)
        assertThat(retrieved1).isNotEqualTo(retrieved2)
    }

    @Test
    fun testDataDeletion() = runTest {
        val testKey = "test_key"
        val testData = "test_data".toByteArray()
        
        // Store and then delete data
        secureStorage.storeSensitiveData(testKey, testData)
        secureStorage.removeSensitiveData(testKey)
        
        // Verify data is deleted
        val retrieved = secureStorage.retrieveSensitiveData(testKey)
        assertThat(retrieved).isNull()
    }

    @Test
    fun testKeyValidation() = runTest {
        val validKey = apiKeyProvider.getApiKey()
        assertThat(apiKeyProvider.validateKey(validKey)).isTrue()
        
        val invalidKey = "invalid_key"
        assertThat(apiKeyProvider.validateKey(invalidKey)).isFalse()
    }
} 