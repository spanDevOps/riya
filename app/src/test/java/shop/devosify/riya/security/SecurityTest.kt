@RunWith(MockitoJUnitRunner::class)
class SecurityTest {
    @Mock private lateinit var secureStorage: SecureStorage
    @Mock private lateinit var encryptionService: EncryptionService
    
    private lateinit var securityManager: SecurityManager

    @Before
    fun setup() {
        securityManager = SecurityManager(
            secureStorage = secureStorage,
            encryptionService = encryptionService
        )
    }

    @Test
    fun `test key rotation`() = runTest {
        // Given
        val oldKey = "old_key"
        val newKey = "new_key"
        whenever(secureStorage.getEncrypted(KEY_ALIAS)).thenReturn(oldKey)

        // When
        securityManager.rotateKey()

        // Then
        verify(secureStorage).storeEncrypted(KEY_ALIAS, newKey)
        verify(encryptionService).reEncryptData(oldKey, newKey)
    }

    @Test
    fun `test secure data storage`() = runTest {
        // Given
        val sensitiveData = "sensitive_info"
        val encrypted = "encrypted_data"
        whenever(encryptionService.encrypt(sensitiveData)).thenReturn(encrypted)

        // When
        securityManager.storeSensitiveData(sensitiveData)

        // Then
        verify(secureStorage).storeEncrypted(any(), eq(encrypted))
    }
} 