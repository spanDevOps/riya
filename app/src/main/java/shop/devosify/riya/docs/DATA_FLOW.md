# Riya - Data Flow Documentation

## Core Interaction Flow

### 1. Wake Word Detection → Voice Input

```mermaid
sequenceDiagram
    WakeWordService->>RiyaForegroundService: "Hey Riya" detected
    RiyaForegroundService->>AudioRecordingService: Start recording
    AudioRecordingService->>VoiceActivityDetector: Monitor speech
    VoiceActivityDetector->>RiyaForegroundService: Speech ended
    RiyaForegroundService->>WhisperRepository: Process audio
```

### 2. Voice Processing → Response Generation

```mermaid
sequenceDiagram
    WhisperRepository->>AssistantsRepository: Text transcription
    AssistantsRepository->>MemoryRetrievalService: Get relevant context
    MemoryRetrievalService->>AssistantsRepository: Context data
    AssistantsRepository->>OpenAI: Send enriched prompt
    OpenAI->>AssistantsRepository: Response
    AssistantsRepository->>MemoryExtractionService: Extract crucial info
    MemoryExtractionService->>MemoryDao: Store memory
```

### 3. Response → User Interaction

```mermaid
sequenceDiagram
    AssistantsRepository->>TtsService: Response text
    TtsService->>OpenAI: TTS request
    OpenAI->>TtsService: Audio stream
    TtsService->>User: Play audio
    EmotionRecognitionService->>AssistantsRepository: Update emotional context
```

## Memory System Flow

### Memory Creation

1. **Extraction**

   - Input: Conversation text
   - Process: GPT analysis
   - Output: Structured memory entities

2. **Storage**
   - Local: Room Database
   - Categories: Facts, Preferences, Habits
   - Priority: Importance-based (1-5)

### Memory Retrieval

1. **Context Building**

   - Input: Current conversation
   - Process: Relevance matching
   - Output: Contextual summary

2. **Usage**
   - Conversation enrichment
   - Response personalization
   - Behavioral adaptation

## Vision System Flow

### Camera Processing

1. **Input Sources**

   - Front camera: User emotion
   - Back camera: Environment

2. **Analysis Pipeline**

   - Frame capture
   - GPT-4 Vision analysis
   - Context integration

3. **Usage**
   - Emotional adaptation
   - Visual understanding
   - Environmental awareness

## System Integration

### Smart Home Control

```mermaid
sequenceDiagram
    VoiceCommand->>VoiceCommandProcessor: Command detected
    VoiceCommandProcessor->>SmartHomeService: Parse command
    SmartHomeService->>Device: Execute action
    Device->>SmartHomeService: Status update
    SmartHomeService->>User: Confirmation
```

### System Monitoring

1. **Metrics Collection**

   - Battery status
   - Network state
   - Device health

2. **Usage**
   - Resource optimization
   - User notifications
   - Performance tuning
