# Riya API Integration Guide

## Overview

Riya integrates with several APIs to provide its functionality:

### 1. OpenAI APIs

- GPT-4 for conversation
- Whisper for speech recognition
- TTS for voice synthesis
- Assistants API for context management

### 2. Voice Processing

- Porcupine for wake word detection
- Local VAD for voice activity detection

### 3. System Integration

- Android TTS as fallback
- System services for device control
- Matter protocol for smart home

## Implementation Details

### Authentication

```kotlin
object ApiConfig {
    const val BASE_URL = "https://api.openai.com/v1/"
    const val API_VERSION = "2024-02-15"

    // Endpoints
    const val CHAT_ENDPOINT = "chat/completions"
    const val WHISPER_ENDPOINT = "audio/transcriptions"
    const val TTS_ENDPOINT = "audio/speech"
    const val ASSISTANTS_ENDPOINT = "assistants"
}

interface ApiKeyProvider {
    fun getApiKey(): String
    fun rotateKey()
    fun validateKey(key: String): Boolean
}
```

### 2. Service Definitions

#### GPT Integration

```kotlin
interface GptApiService {
    @POST("chat/completions")
    suspend fun getResponseFromGpt(
        @Body request: GptRequest
    ): Response<GptResponse>

    @POST("chat/completions")
    suspend fun generateVisionResponse(
        @Body request: VisionRequest
    ): Response<GptResponse>
}

data class GptRequest(
    val model: String = "gpt-4-turbo-preview",
    val messages: List<Message>,
    val temperature: Float = 0.7f,
    val max_tokens: Int = 500
)
```

#### Whisper Integration

```kotlin
interface WhisperApiService {
    @Multipart
    @POST("audio/transcriptions")
    suspend fun transcribeAudio(
        @Part file: MultipartBody.Part,
        @Part("model") model: RequestBody = "whisper-1".toRequestBody(),
        @Part("language") language: RequestBody = "en".toRequestBody()
    ): Response<WhisperResponse>
}

data class WhisperResponse(
    val text: String,
    val segments: List<Segment>?,
    val language: String
)
```

#### TTS Integration

```kotlin
interface TtsApiService {
    @POST("audio/speech")
    suspend fun generateSpeech(
        @Body request: TtsRequest
    ): Response<ResponseBody>
}

data class TtsRequest(
    val model: String = "tts-1",
    val input: String,
    val voice: String = "nova",
    val speed: Float = 1.0f,
    val format: String = "mp3"
)
```

### 3. Error Handling

```kotlin
sealed class ApiError : Exception() {
    data class NetworkError(
        override val message: String,
        val code: Int
    ) : ApiError()

    data class RateLimitError(
        val retryAfter: Int
    ) : ApiError()

    data class AuthenticationError(
        override val message: String
    ) : ApiError()

    data class ServiceError(
        override val message: String,
        val serviceType: String
    ) : ApiError()
}

class ApiErrorHandler {
    fun handleError(error: ApiError): ApiResponse<Unit> {
        return when (error) {
            is ApiError.RateLimitError -> {
                scheduleRetry(error.retryAfter)
                ApiResponse.Error("Rate limit exceeded")
            }
            is ApiError.NetworkError -> {
                if (error.code in 500..599) {
                    retryWithBackoff()
                }
                ApiResponse.Error("Network error: ${error.message}")
            }
            is ApiError.AuthenticationError -> {
                rotateApiKey()
                ApiResponse.Error("Authentication failed")
            }
            is ApiError.ServiceError -> {
                logServiceError(error)
                ApiResponse.Error("Service error: ${error.message}")
            }
        }
    }
}
```

### 4. Rate Limiting

```kotlin
class RateLimiter {
    private val limits = mapOf(
        "gpt" to 3000,      // 3000 RPM
        "whisper" to 50,    // 50 RPM
        "tts" to 100        // 100 RPM
    )

    private val tokenBuckets = mutableMapOf<String, TokenBucket>()

    fun checkLimit(service: String): Boolean {
        return tokenBuckets[service]?.tryConsume() ?: false
    }

    fun updateQuota(headers: Headers) {
        // Update remaining tokens based on API response headers
    }
}
```

### 5. Caching Strategy

```kotlin
interface ApiCache {
    suspend fun get(key: String): CachedResponse?
    suspend fun put(key: String, response: CachedResponse)
    suspend fun invalidate(key: String)
    suspend fun clear()
}

data class CachedResponse(
    val data: String,
    val timestamp: Long,
    val expiresIn: Long
)

class ApiCacheImpl : ApiCache {
    private val memoryCache = Cache.Builder()
        .maximumSize(1000)
        .expireAfterWrite(1, TimeUnit.HOURS)
        .build<String, CachedResponse>()
}
```

### 6. Network Security

```kotlin
object NetworkSecurity {
    fun configureTls(): SSLContext {
        return SSLContext.getInstance("TLS").apply {
            init(null, getTrustManagers(), SecureRandom())
        }
    }

    fun getCertificatePinner(): CertificatePinner {
        return CertificatePinner.Builder()
            .add("api.openai.com", "sha256/...")
            .build()
    }
}
```

### 7. Testing

```kotlin
class ApiIntegrationTest {
    @Test
    fun testGptIntegration() {
        runBlocking {
            val response = gptService.getResponseFromGpt(
                GptRequest(prompt = "Test prompt")
            )
            assertThat(response.isSuccessful).isTrue()
        }
    }

    @Test
    fun testWhisperIntegration() {
        runBlocking {
            val audioFile = createTestAudioFile()
            val response = whisperService.transcribeAudio(audioFile)
            assertThat(response.text).isNotEmpty()
        }
    }
}
```

## Best Practices

1. **API Key Management**

   - Rotate keys regularly
   - Store securely using encryption
   - Use different keys for dev/prod
   - Monitor usage and costs

2. **Error Recovery**

   - Implement exponential backoff
   - Cache responses when appropriate
   - Provide offline fallbacks
   - Log errors for analysis

3. **Performance**

   - Use connection pooling
   - Implement request compression
   - Cache responses
   - Monitor latency

4. **Security**
   - Use TLS 1.3
   - Validate certificates
   - Sanitize inputs
   - Encrypt sensitive data

## Model Selection

Riya uses OpenAI's language models with the following configuration:

### Default Model

- GPT-3.5-turbo
- Balanced performance and cost
- Suitable for most interactions

### Optional GPT-4

- Available in settings
- Enhanced capabilities
- Higher API cost
- Recommended for complex tasks

### Model Selection Logic

```kotlin
val model = when {
    forceGpt4 -> "gpt-4-turbo-preview"  // For specific features
    userEnabled -> "gpt-4-turbo-preview" // User preference
    else -> "gpt-3.5-turbo"             // Default
}
```
