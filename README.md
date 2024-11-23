# Riya - Intelligent AI Assistant

## Overview

Riya is a sophisticated AI assistant that combines cloud intelligence with local processing capabilities to provide a seamless, privacy-focused user experience.

## Key Features

### 🎯 Core Capabilities

- Voice interaction with wake word "Hey Riya"
- Natural language processing (GPT-3.5/4)
- Offline command processing
- Context-aware responses
- Cross-modal memory system

### 🤖 Intelligence Features

- Pattern recognition
- Learning from interactions
- Proactive assistance
- Context awareness
- Behavioral adaptation

### 🛠 System Integration

- Smart home control
- Device management
- App automation
- System settings

### 🛍 Shopping Assistant

- Amazon/Flipkart integration
- Price tracking
- Order management
- Smart recommendations
- Deal finding

### 📱 Personal Management

- Calendar integration
- Task management
- Email handling
- Contact management
- Social connections

### 🏠 Home Automation

- Device control
- Scene management
- Environmental optimization
- Energy management

## Setup

### Prerequisites

```bash
# Required
- Android Studio Arctic Fox or newer
- Kotlin 1.8+
- Android SDK 26+
- OpenAI API key
- Porcupine wake word key
```

### Installation

1. Clone the repository

```bash
git clone https://github.com/yourusername/riya.git
```

2. Create local.properties with API keys

```properties
OPENAI_API_KEY=your_key_here
PORCUPINE_KEY=your_key_here
```

3. Build and run

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

### Data Flow

1. Voice Input → Wake Word Detection → Speech Recognition
2. Text Processing → Context Building → Response Generation
3. Action Execution → Memory Storage → Pattern Learning

## Usage

### Voice Commands

- "Hey Riya, what's the weather?"
- "Hey Riya, turn on living room lights"
- "Hey Riya, order my usual coffee"
- "Hey Riya, remind me to call mom at 5"

### Shopping

- "Find me a gaming laptop under 80000"
- "Track prices for iPhone 15"
- "Reorder coffee pods"
- "Show my order status"

### Automation

- "Configure new task for movie time"
- "Create morning routine"
- "Set up work mode"
- "Automate evening lights"

## Security

### Data Protection

- Encrypted storage
- Secure key management
- Regular key rotation
- Privacy controls

### API Security

- Token management
- Certificate pinning
- Request signing
- Rate limiting

## Contributing

### Development Flow

1. Fork repository
2. Create feature branch
3. Implement changes
4. Add tests
5. Submit pull request

### Testing

```bash
# Run all tests
./gradlew test

# Run specific tests
./gradlew :app:testDebugUnitTest --tests "*.MemorySystemTest"
```

## Documentation

### Technical Guides

- [Architecture Guide](docs/ARCHITECTURE.md)
- [API Integration](docs/API_INTEGRATION.md)
- [Security Guide](docs/SECURITY.md)
- [Testing Guide](docs/TESTING.md)

### User Guides

- [Getting Started](docs/GETTING_STARTED.md)
- [Voice Commands](docs/VOICE_COMMANDS.md)
- [Shopping Guide](docs/SHOPPING.md)
- [Automation Guide](docs/AUTOMATION.md)

## License

[License details here]

## Support

[Support information here]
