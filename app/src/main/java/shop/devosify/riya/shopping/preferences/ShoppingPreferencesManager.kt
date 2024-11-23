@Singleton
class ShoppingPreferencesManager @Inject constructor(
    private val secureStorage: SecureStorage,
    private val memoryDao: MemoryDao,
    private val gptRepository: GptRepository
) {
    suspend fun updatePreferences(input: String) {
        // Use GPT to understand preference
        val preference = analyzePreference(input)
        storePreference(preference)
    }

    suspend fun getPreferences(): ShoppingPreferences {
        return ShoppingPreferences(
            preferredPlatform = secureStorage.getString(PREF_PLATFORM) ?: DEFAULT_PLATFORM,
            priceRanges = loadPriceRanges(),
            brandPreferences = loadBrandPreferences(),
            qualityPreferences = loadQualityPreferences(),
            deliveryPreferences = loadDeliveryPreferences()
        )
    }

    private suspend fun analyzePreference(input: String): ShoppingPreference {
        val prompt = """
            Analyze this shopping preference:
            "$input"
            
            Extract:
            1. Category (platform/price/brand/quality/delivery)
            2. Specific preference
            3. Strength of preference (1-5)
            
            Return JSON preference object
        """.trimIndent()

        return gptRepository.generateText(prompt)
            .map { gson.fromJson(it, ShoppingPreference::class.java) }
            .getOrThrow()
    }

    private suspend fun storePreference(preference: ShoppingPreference) {
        // Store in secure storage
        secureStorage.storeObject(PREF_KEY_PREFIX + preference.category, preference)
        
        // Create memory
        memoryDao.insertMemory(MemoryEntity(
            type = MemoryType.SHOPPING_PREFERENCE,
            content = "Shopping preference: ${preference.description}",
            importance = preference.strength,
            metadata = mapOf(
                "category" to preference.category,
                "value" to preference.value
            )
        ))
    }
}

data class ShoppingPreferences(
    val preferredPlatform: String,
    val priceRanges: Map<String, PriceRange>,
    val brandPreferences: Map<String, List<String>>,
    val qualityPreferences: QualityPreferences,
    val deliveryPreferences: DeliveryPreferences
) 