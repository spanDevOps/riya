# Riya - Advanced AI Assistant

## Overview

Riya is a sophisticated AI assistant that combines local intelligence with cloud capabilities to provide a seamless, privacy-focused user experience.

## Key Features

### 🎯 Core Capabilities

- **Voice Interaction**

  - Battery-optimized wake word detection ("Hey Riya")
  - Natural language processing via GPT-3.5 (GPT-4 optional)
  - High-quality voice synthesis (OpenAI TTS)
  - Offline command support

- **Intelligent Memory**

  - Cross-modal memory system
  - Context-aware retrieval
  - Pattern recognition
  - Long-term learning

- **System Integration**
  - Smart home control
  - Device settings management
  - Calendar & email integration
  - Location awareness

### 🛡️ Privacy & Security

- Local data encryption
- Secure API key management
- Regular key rotation
- Privacy-first design
- Configurable data retention

### 🔋 Performance

- Battery-optimized wake word detection
- Efficient memory management
- Smart caching system
- Adaptive resource usage

## Setup

1. **Prerequisites**

   - Android Studio Arctic Fox or newer
   - Android SDK 26+
   - Kotlin 1.8+

2. **API Keys**

   ```properties
   # Create local.properties with:
   OPENAI_API_KEY=your_key_here
   PORCUPINE_KEY=your_key_here
   ```

3. **Build & Run**
   ```bash
   ./gradlew assembleDebug
   ```

## Architecture

### Components

```kotlin
// Core Services
VoicePipeline        // Speech processing
MemorySystem         // Information storage
AutomationEngine     // Task automation
SecurityManager      // Key & data protection

// Integration
SmartHomeService     // Device control
LocationService      // Geofencing
CalendarService      // Schedule management
EmailService         // Communication
```

### Performance Monitoring

```kotlin
// System Health
ResourceMonitor      // Resource tracking
PerformanceTracker   // Metrics collection
ErrorTracker         // Issue monitoring
```

## Model Configuration

### Default Model (GPT-3.5)

- Balanced performance and cost
- Suitable for most interactions
- Fast response times
- Efficient token usage

### Optional GPT-4

- Available in settings
- Enhanced capabilities
- Higher API cost
- Recommended for complex tasks

## Contributing

1. Fork the repository
2. Create feature branch
3. Commit changes
4. Open pull request

## Testing

```bash
# Run all tests
./gradlew test

# Run specific tests
./gradlew :app:testDebugUnitTest --tests "*.MemorySystemTest"
```

## License

[License details here]

## Security

- All API keys must be stored in `local.properties`
- Never commit sensitive data
- Regular security audits
- Encrypted storage

## Documentation

- [Technical Specification](docs/TECHNICAL_SPEC.md)
- [API Integration](docs/API_INTEGRATION.md)
- [Security & Privacy](docs/SECURITY_PRIVACY.md)
- [Memory System](docs/MEMORY_SYSTEM.md)
