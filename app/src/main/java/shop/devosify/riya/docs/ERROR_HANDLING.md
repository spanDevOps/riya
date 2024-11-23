# Error Handling & Recovery Strategies

## Core Principles

### 1. Graceful Degradation

The system maintains functionality through multiple fallback layers:

```kotlin
sealed class FallbackStrategy {
    object LocalCache
    object SystemTTS
    object OfflineMode
    object BasicFunctionality
}

class ServiceDegradation {
    fun getNextFallback(service: String, error: RiyaError): FallbackStrategy {
        return when (service) {
            "voice" -> when (error) {
                is RiyaError.NetworkError -> FallbackStrategy.LocalCache
                is RiyaError.APIError -> FallbackStrategy.SystemTTS
                else -> FallbackStrategy.BasicFunctionality
            }
            "memory" -> when (error) {
                is RiyaError.StorageError -> FallbackStrategy.LocalCache
                else -> FallbackStrategy.OfflineMode
            }
            else -> FallbackStrategy.BasicFunctionality
        }
    }
}
```

### 2. Error Categories & Handling

```kotlin
sealed class RiyaError {
    data class NetworkError(
        val code: Int,
        val message: String,
        val isRetryable: Boolean = true
    ) : RiyaError()

    data class APIError(
        val service: String,
        val message: String,
        val response: String? = null
    ) : RiyaError()

    data class StorageError(
        val operation: String,
        val message: String
    ) : RiyaError()

    data class PermissionError(
        val permission: String,
        val isRequired: Boolean = true
    ) : RiyaError()

    data class HardwareError(
        val device: String,
        val error: String,
        val isCritical: Boolean = false
    ) : RiyaError()
}
```

## Component-Specific Strategies

### 1. Voice Pipeline

```kotlin
class VoicePipelineHandler {
    suspend fun handleError(error: RiyaError): Result<String> {
        return when (error) {
            is RiyaError.NetworkError -> {
                if (error.isRetryable) {
                    withRetry(maxAttempts = 3) {
                        // Retry network operation
                    }
                } else {
                    // Use cached responses
                    getCachedResponse()
                }
            }
            is RiyaError.APIError -> when (error.service) {
                "whisper" -> useLocalSTT()
                "gpt" -> useLocalResponseGeneration()
                "tts" -> useSystemTTS()
                else -> Result.failure(error)
            }
            else -> Result.failure(error)
        }
    }
}
```

### 2. Memory System

```kotlin
class MemorySystemHandler {
    suspend fun handleStorageError(error: StorageError): Result<Unit> {
        return when (error.operation) {
            "write" -> {
                // Attempt cleanup
                cleanupOldMemories()
                // Retry write with reduced data
                retryWriteWithCompression()
            }
            "read" -> {
                // Use cached data
                getCachedMemories()
            }
            "query" -> {
                // Simplify query
                retryWithSimplifiedQuery()
            }
            else -> Result.failure(error)
        }
    }
}
```

## Recovery Mechanisms

### 1. Automatic Recovery

```kotlin
class RecoveryManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    suspend fun <T> withRecovery(
        operation: suspend () -> T,
        fallback: suspend (RiyaError) -> T,
        maxRetries: Int = 3
    ): T {
        var lastError: RiyaError? = null
        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastError = e.toRiyaError()
                delay(calculateBackoff(attempt))
            }
        }
        return fallback(lastError ?: RiyaError.Unknown)
    }

    private fun calculateBackoff(attempt: Int): Long {
        return min(1000L * (2.0.pow(attempt)).toLong(), 30000L)
    }
}
```

### 2. State Recovery

```kotlin
class StateManager {
    private val stateStore = StateStore()

    suspend fun saveState(state: AppState) {
        stateStore.save(state)
    }

    suspend fun recoverState(): AppState {
        return stateStore.getLatestValid() ?: AppState.createDefault()
    }

    suspend fun validateState(state: AppState): Boolean {
        return state.isValid() && state.isConsistent()
    }
}
```

## Error Reporting & Analytics

```kotlin
class ErrorReporter {
    fun logError(
        error: RiyaError,
        context: Map<String, Any> = emptyMap(),
        severity: ErrorSeverity = ErrorSeverity.NORMAL
    ) {
        when (severity) {
            ErrorSeverity.CRITICAL -> {
                analyticsService.logCriticalError(error)
                notifyDevelopers(error)
            }
            ErrorSeverity.NORMAL -> {
                analyticsService.logError(error)
            }
            ErrorSeverity.LOW -> {
                analyticsService.logWarning(error)
            }
        }
    }
}
```

## Testing Error Scenarios

```kotlin
@Test
fun testVoicePipelineRecovery() = runTest {
    // Given
    val networkError = RiyaError.NetworkError(
        code = 503,
        message = "Service unavailable"
    )

    // When
    voicePipelineHandler.handleError(networkError)

    // Then
    verify(exactly = 1) {
        // Verify fallback to local cache
        cacheService.getLocalResponse()
    }
}
```

## Best Practices

1. **Error Prevention**

   - Input validation
   - Resource checks
   - Permission verification
   - Network status monitoring

2. **Recovery Priority**

   - Critical features first
   - Data consistency
   - User experience
   - Resource cleanup

3. **User Communication**
   - Clear error messages
   - Recovery options
   - Progress updates
   - Success confirmation
