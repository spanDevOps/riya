@Singleton
class TranslateService @Inject constructor(
    private val translateApiService: TranslateApiService,
    private val networkUtils: NetworkUtils,
    private val offlineManager: OfflineManager
) {
    private val mlKitTranslator = MLKitTranslator() // Local fallback

    suspend fun translate(
        text: String,
        sourceLanguage: Language,
        targetLanguage: Language
    ): String {
        return if (networkUtils.isNetworkAvailable()) {
            // Use OpenAI for better translation quality
            try {
                val prompt = """
                    Translate this text from ${sourceLanguage.displayName} to ${targetLanguage.displayName}:
                    $text
                    
                    Maintain the original meaning, tone, and cultural context.
                    Return only the translated text without any explanations.
                """.trimIndent()

                gptRepository.generateText(prompt)
                    .getOrElse { 
                        // Fallback to ML Kit if OpenAI fails
                        mlKitTranslator.translate(text, sourceLanguage, targetLanguage)
                    }
            } catch (e: Exception) {
                mlKitTranslator.translate(text, sourceLanguage, targetLanguage)
            }
        } else {
            // Offline translation using ML Kit
            mlKitTranslator.translate(text, sourceLanguage, targetLanguage)
        }
    }

    suspend fun detectLanguage(text: String): Language {
        return if (networkUtils.isNetworkAvailable()) {
            translateApiService.detectLanguage(text)
                .map { Language.fromCode(it.language) ?: Language.ENGLISH }
                .getOrElse { mlKitTranslator.detectLanguage(text) }
        } else {
            mlKitTranslator.detectLanguage(text)
        }
    }
} 