@Singleton
class LanguageManager @Inject constructor(
    private val secureStorage: SecureStorage,
    private val translateService: TranslateService,
    private val analyticsService: AnalyticsService
) {
    private val _currentLanguage = MutableStateFlow(Language.ENGLISH)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    init {
        // Load saved language preference
        scope.launch {
            val savedLanguage = secureStorage.getString(PREF_LANGUAGE)
            _currentLanguage.value = Language.fromCode(savedLanguage) ?: Language.ENGLISH
        }
    }

    suspend fun setLanguage(language: Language) {
        _currentLanguage.value = language
        secureStorage.putString(PREF_LANGUAGE, language.code)
        analyticsService.logEvent("language_changed", mapOf(
            "language" to language.code,
            "is_system_language" to (language == getSystemLanguage())
        ))
    }

    suspend fun detectLanguage(text: String): Language {
        return translateService.detectLanguage(text)
    }

    suspend fun translateToEnglish(text: String, sourceLanguage: Language? = null): String {
        if (sourceLanguage == Language.ENGLISH) return text
        
        val detectedLanguage = sourceLanguage ?: detectLanguage(text)
        return translateService.translate(
            text = text,
            sourceLanguage = detectedLanguage,
            targetLanguage = Language.ENGLISH
        )
    }

    suspend fun translateFromEnglish(text: String, targetLanguage: Language): String {
        if (targetLanguage == Language.ENGLISH) return text
        
        return translateService.translate(
            text = text,
            sourceLanguage = Language.ENGLISH,
            targetLanguage = targetLanguage
        )
    }

    companion object {
        private const val PREF_LANGUAGE = "preferred_language"
    }
}

enum class Language(
    val code: String,
    val displayName: String,
    val nativeNames: List<String>
) {
    ENGLISH(
        code = "en",
        displayName = "English",
        nativeNames = listOf("English")
    ),
    SPANISH(
        code = "es",
        displayName = "Spanish",
        nativeNames = listOf("Español", "Castellano", "Spanish")
    ),
    FRENCH(
        code = "fr",
        displayName = "French",
        nativeNames = listOf("Français", "French")
    ),
    GERMAN(
        code = "de",
        displayName = "German",
        nativeNames = listOf("Deutsch", "German")
    ),
    CHINESE(
        code = "zh",
        displayName = "Chinese",
        nativeNames = listOf("中文", "Chinese", "Mandarin", "汉语", "普通话")
    ),
    JAPANESE(
        code = "ja",
        displayName = "Japanese",
        nativeNames = listOf("日本語", "Japanese")
    ),
    KOREAN(
        code = "ko",
        displayName = "Korean",
        nativeNames = listOf("한국어", "Korean")
    ),
    HINDI(
        code = "hi",
        displayName = "Hindi",
        nativeNames = listOf("हिन्दी", "Hindi")
    );

    companion object {
        fun fromCode(code: String?): Language? = values().find { it.code == code }
        fun fromName(name: String): Language? = values().find { language ->
            language.displayName.equals(name, ignoreCase = true) ||
            language.nativeNames.any { it.equals(name, ignoreCase = true) }
        }
    }
} 