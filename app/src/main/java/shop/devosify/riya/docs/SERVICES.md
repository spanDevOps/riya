# Riya Services Documentation

## Voice Services

### WakeWordService

- Purpose: Continuous wake word detection ("Hey Riya")
- Implementation: Porcupine SDK
- Key Features:
  - Low power consumption
  - High accuracy (>95%)
  - Customizable wake word
- Configuration:
  ```kotlin
  sampleRate = 16000Hz
  encoding = PCM_16BIT
  channels = MONO
  ```

### VoiceActivityDetector

- Purpose: Detect speech boundaries
- Implementation: Custom energy-based detection
- Parameters:
  - Silence threshold: 500
  - End-of-speech delay: 1000ms
  - Buffer size: Dynamic
- Features:
  - Real-time processing
  - Adaptive thresholding
  - Noise filtering

### AudioRecordingService

- Purpose: High-quality audio capture
- Format:
  - Sample Rate: 16kHz
  - Channels: Mono
  - Encoding: PCM 16-bit
- Features:
  - Amplitude visualization
  - Temporary file management
  - Auto cleanup

## AI Services

### AssistantsService

- Purpose: OpenAI Assistants API integration
- Features:
  - Thread management
  - Context preservation
  - Function calling
  - Memory integration
- Key Methods:
  ```kotlin
  createAssistant()
  createThread()
  sendMessage()
  createAndWaitForRun()
  ```

### TtsService

- Purpose: Text-to-speech synthesis
- Implementation: OpenAI TTS API
- Voices:
  - Primary: Nova
  - Alternatives: Alloy, Echo, Fable, Onyx, Shimmer
- Features:
  - High-quality synthesis
  - Emotion-aware modulation
  - Fallback to system TTS

### EmotionRecognitionService

- Purpose: Real-time emotion analysis
- Components:
  - ML Kit face detection
  - GPT-4 Vision analysis
  - Expression classification
- Features:
  - Multi-face tracking
  - Confidence scoring
  - Temporal smoothing

## Vision Services

### CameraVisionService

- Purpose: Real-time visual understanding
- Implementation:
  - CameraX for capture
  - GPT-4 Vision for analysis
- Features:
  - Dual camera support
  - Scene understanding
  - Object detection
  - Text recognition

## Memory Services

### MemoryExtractionService

- Purpose: Extract crucial information
- Implementation: GPT-powered analysis
- Features:
  - Importance ranking
  - Category classification
  - Context preservation
- Categories:
  ```kotlin
  - Facts
  - Preferences
  - Habits
  - Goals
  - Relationships
  ```

### MemoryRetrievalService

- Purpose: Context-aware memory access
- Features:
  - Relevance scoring
  - Priority queuing
  - Dynamic context window
- Methods:
  ```kotlin
  getRelevantContext()
  getImportantMemories()
  searchMemories()
  ```

## System Services

### SystemMonitorService

- Purpose: Device state monitoring
- Metrics:
  - Battery level/state
  - Network connectivity
  - Bluetooth devices
  - System settings
- Features:
  - Real-time updates
  - State change notifications
  - Resource optimization

### SmartHomeService

- Purpose: IoT device control
- Protocol: Matter
- Features:
  - Device discovery
  - State management
  - Command execution
  - Status monitoring

## Analytics Services

### AnalyticsService

- Purpose: Usage and performance tracking
- Metrics:
  - Voice interaction success
  - Response times
  - Memory utilization
  - Error rates
- Features:
  - Firebase integration
  - Custom event tracking
  - Error reporting
  - Performance monitoring
