# API Integration Guide

## Overview

Riya integrates with multiple external APIs to provide its functionality:

### Core APIs

1. OpenAI (GPT, Whisper, TTS)
2. Shopping Platforms (Amazon, Flipkart)
3. Smart Home Services
4. Cloud Services

## OpenAI Integration

### 1. GPT Integration

```kotlin
interface GptService {
    suspend fun generateResponse(
        prompt: String,
        context: Map<String, Any>? = null,
        model: String = "gpt-3.5-turbo" // or "gpt-4" if enabled
    ): Result<String>
}

// Implementation
class GptServiceImpl(
    private val apiKey: String,
    private val modelConfig: ModelConfig
) {
    suspend fun generateResponse(prompt: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.createCompletion(
                    model = modelConfig.currentModel,
                    messages = buildMessages(prompt),
                    temperature = 0.7,
                    maxTokens = 500
                )
                Result.success(response.choices.first().message.content)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
```

### 2. Whisper Integration

```kotlin
interface WhisperService {
    suspend fun transcribeAudio(
        audioFile: File,
        language: String? = null
    ): Result<String>
}

// Usage
val transcription = whisperService.transcribeAudio(
    audioFile = recordedAudio,
    language = "en"
)
```

### 3. TTS Integration

```kotlin
interface TtsService {
    suspend fun generateSpeech(
        text: String,
        voice: String = "nova"
    ): Result<File>
}
```

## Shopping Platform Integration

### 1. Amazon Integration

```kotlin
interface AmazonService {
    suspend fun searchProducts(query: String): List<Product>
    suspend fun getProductDetails(productId: String): Product
    suspend fun placeOrder(order: Order): OrderResult
    suspend fun trackOrder(orderId: String): OrderStatus
}

// Authentication
class AmazonAuthenticator {
    suspend fun authenticate(): AuthResult
    suspend fun refreshToken(): AuthResult
}
```

### 2. Flipkart Integration

```kotlin
interface FlipkartService {
    suspend fun searchProducts(query: String): List<Product>
    suspend fun getProductDetails(productId: String): Product
    suspend fun placeOrder(order: Order): OrderResult
    suspend fun trackOrder(orderId: String): OrderStatus
}
```

## Smart Home Integration

### 1. Device Control

```kotlin
interface SmartHomeService {
    suspend fun getDevices(): List<SmartDevice>
    suspend fun controlDevice(deviceId: String, command: DeviceCommand)
    suspend fun getDeviceStatus(deviceId: String): DeviceStatus
}

// Implementation example
class SmartHomeServiceImpl {
    suspend fun controlDevice(deviceId: String, command: DeviceCommand) {
        when (command) {
            is PowerCommand -> handlePowerCommand(deviceId, command)
            is BrightnessCommand -> handleBrightnessCommand(deviceId, command)
            is ColorCommand -> handleColorCommand(deviceId, command)
        }
    }
}
```

## Error Handling

### 1. Rate Limiting

```kotlin
class RateLimiter {
    private val limits = mapOf(
        "gpt" to 3000,      // requests per minute
        "whisper" to 50,
        "tts" to 100
    )

    suspend fun checkLimit(service: String): Boolean
    suspend fun handleRateLimit(service: String)
}
```

### 2. Error Recovery

```kotlin
sealed class ApiError {
    data class NetworkError(val code: Int): ApiError()
    data class RateLimit(val retryAfter: Int): ApiError()
    data class AuthError(val message: String): ApiError()
}

class ErrorHandler {
    suspend fun handleError(error: ApiError): Result<Unit> {
        return when (error) {
            is ApiError.RateLimit -> handleRateLimit(error)
            is ApiError.NetworkError -> handleNetworkError(error)
            is ApiError.AuthError -> handleAuthError(error)
        }
    }
}
```

## Authentication

### 1. Token Management

```kotlin
class TokenManager {
    suspend fun getValidToken(service: String): String {
        val token = getStoredToken(service)
        return if (isTokenValid(token)) {
            token
        } else {
            refreshToken(service)
        }
    }
}
```

### 2. Secure Storage

```kotlin
interface SecureStorage {
    suspend fun storeToken(service: String, token: String)
    suspend fun getToken(service: String): String?
    suspend fun clearToken(service: String)
}
```

## Performance Optimization

### 1. Caching

```kotlin
class ApiCache {
    private val cache = LruCache<String, CachedResponse>(1000)

    suspend fun get(key: String): CachedResponse?
    suspend fun put(key: String, response: CachedResponse)
    suspend fun invalidate(key: String)
}
```

### 2. Request Batching

```kotlin
class RequestBatcher {
    suspend fun batchRequests(
        requests: List<ApiRequest>
    ): List<ApiResponse>
}
```

## Testing

### 1. Mock Responses

```kotlin
class MockApiService : ApiService {
    override suspend fun makeRequest(
        endpoint: String,
        params: Map<String, Any>
    ): ApiResponse {
        return getMockResponse(endpoint)
    }
}
```

### 2. Integration Testing

```kotlin
@Test
fun testApiIntegration() = runTest {
    val response = apiService.makeRequest(
        endpoint = "test_endpoint",
        params = mapOf("param" to "value")
    )
    assertThat(response.isSuccessful).isTrue()
}
```
