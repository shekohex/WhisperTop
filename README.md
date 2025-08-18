# WhisperTop

**Your voice, anywhere.**

WhisperTop is an Android overlay application that enables quick and accurate speech-to-text transcription anywhere on your device. With a floating microphone button that works on top of any app, you can transcribe speech directly into text fields using your own OpenAI API key.

## Overview

WhisperTop provides seamless speech-to-text functionality through a system-wide overlay that doesn't interfere with your existing keyboard or input methods. The app uses OpenAI's Whisper API for high-quality transcription, supporting multiple models including Whisper 3 Turbo and GPT-4o.

**Key Benefits:**
- Works in any app without replacing your keyboard
- Privacy-first: uses your own OpenAI API key
- No data retention or logging
- Fast, accurate transcription with multi-language support
- Customizable floating interface

## Features

### Core Features
- **Floating Microphone Button**: Draggable overlay button accessible from any app
- **Intelligent Text Insertion**: Transcribed text automatically appears in active text fields with smart spacing
- **Text Replacement vs. Appending**: Replaces selected text or intelligently appends to existing content
- **OpenAI Integration**: Support for Whisper 3 Turbo, GPT-4o, and other transcription models
- **Multi-language Support**: Automatic language detection or manual selection
- **Secure Storage**: API keys encrypted locally using Android's EncryptedSharedPreferences
- **Visual Feedback**: Color-coded states (idle, recording, processing) with animated indicators
- **Custom Endpoints**: Support for OpenAI-compatible transcription services
- **Transcription Customization**: Custom prompts and temperature control for improved accuracy
- **Clickable Permission Cards**: Intuitive onboarding with interactive permission requests

### Privacy & Security
- All transcription requests sent directly to OpenAI using your API key
- No audio files or transcriptions stored on device
- No third-party intermediaries or data sharing
- Complete control over your data and API usage

## Installation

### Prerequisites
- Android 8.0 (API level 26) or higher
- OpenAI API account with available credits
- Device with microphone access

### Download
1. Download the latest APK from the [Releases](https://github.com/shekohex/WhisperTop/releases) page
2. Enable "Install from Unknown Sources" in your device settings
3. Install the APK

### Required Permissions
WhisperTop requires the following permissions to function:

- **Microphone**: To capture audio for transcription
- **Display over other apps**: For the floating microphone button
- **Accessibility Service**: To insert transcribed text into active fields

You'll be prompted to grant these permissions during initial setup.

## Setup

### 1. API Configuration
1. Open WhisperTop and navigate to Settings
2. Enter your OpenAI API key
3. (Optional) Configure a custom endpoint if using an OpenAI-compatible service
4. Select your preferred transcription model:
   - Whisper 3 Turbo (faster, cost-effective)
   - GPT-4o (higher accuracy)
   - Custom model (manual entry)

### 2. Enable Overlay
1. Grant "Display over other apps" permission when prompted
2. The floating microphone button will appear on your screen
3. Drag it to your preferred position

### 3. Enable Accessibility Service
1. Go to Android Settings > Accessibility
2. Find "WhisperTop" in the list
3. Enable the accessibility service
4. This allows the app to insert transcribed text into active fields

## Usage

### Basic Operation
1. **Start Recording**: Tap the floating microphone button
2. **Speak Clearly**: The button will show a pulsing animation while recording
3. **Stop Recording**: Tap the button again or wait for auto-stop
4. **Automatic Insertion**: Transcribed text appears in the currently focused text field

### Visual Indicators
- **Gray**: Idle state, ready to record
- **Red**: Actively recording audio
- **Blue**: Processing audio with OpenAI API
- **Green**: Successfully completed transcription

### Tips for Best Results
- Speak clearly and at a moderate pace
- Ensure good audio quality (minimal background noise)
- Position your device's microphone appropriately
- Use in quiet environments when possible

## Configuration Options

### API Settings
- **API Key**: Your OpenAI API key (stored securely)
- **Base URL**: Custom endpoint for OpenAI-compatible services
- **Model Selection**: Choose from available transcription models
- **Language**: Auto-detect or specify a target language

### Transcription Customization
- **Custom Prompt**: Provide context to improve transcription accuracy for domain-specific terms, names, or technical vocabulary (up to ~224 tokens)
- **Temperature Control**: Adjust transcription randomness:
  - **0.0-0.3**: More accurate and consistent results
  - **0.4-0.7**: Balanced approach 
  - **0.8-2.0**: More creative interpretations (may introduce variations)

These advanced settings allow you to fine-tune the OpenAI Whisper API behavior for your specific use case, improving accuracy for specialized vocabulary or adjusting the consistency of transcriptions.

### Interface Settings
- **Overlay Position**: Drag the button to your preferred location
- **Button Size**: Adjust overlay button size (future feature)
- **Theme Options**: Light/dark mode support (future feature)

## Development

### Tech Stack
- **Language**: Kotlin 2.0 with K2 compiler
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: Clean Architecture with Kotlin Multiplatform
- **Minimum SDK**: 26 (Android 8.0)
- **Target SDK**: 35 (Android 15)
- **Build System**: Gradle with Android Gradle Plugin 8.12

### Building from Source

#### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17 or higher
- Android SDK with API 35

#### Build Commands
```bash
# Clone the repository
git clone https://github.com/shekohex/WhisperTop.git
cd WhisperTop

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug

# Run tests
./gradlew test

# Run lint checks
./gradlew lint
```

### Project Structure
```
├── composeApp/          # Android-specific UI and platform features
├── shared/              # Cross-platform business logic and data layer
├── iosApp/              # iOS entry point (future iOS support)
└── gradle/              # Build configuration
```

### Architecture Overview
The project follows Clean Architecture principles with three main layers:

- **Domain Layer**: Core business logic and use cases
- **Data Layer**: API clients, repositories, and data storage
- **Presentation Layer**: Android UI, services, and view models

Key components:
- **AudioRecordingService**: Foreground service for microphone capture
- **OverlayService**: System overlay management for floating UI
- **WhisperTopAccessibilityService**: Text insertion via accessibility API

## Roadmap

### Phase 1 (Current - P0 Features)
- [x] Floating overlay microphone button
- [x] Audio recording and WAV file generation
- [x] OpenAI API integration
- [x] Intelligent text insertion with smart spacing
- [x] Text selection handling (replace vs. append)
- [x] Accessibility-based text insertion
- [x] Secure API key storage
- [x] Interactive onboarding with clickable permission cards

### Phase 2 (P1 Features)
- [ ] Quick Settings Tile integration
- [ ] Hardware button push-to-talk support
- [ ] Theme customization (dark/light modes)
- [ ] Manual language and model selection
- [ ] Hotword activation for hands-free mode

### Phase 3 (P2 Features)
- [ ] Offline Whisper inference (faster-whisper)
- [ ] Translation mode (speech-to-text in different languages)
- [ ] Voice command macros for automation
- [ ] iOS support via Kotlin Multiplatform

## Contributing

We welcome contributions! Please feel free to submit issues, feature requests, or pull requests.

### Getting Started
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/your-feature`)
3. Make your changes following the existing code style
4. Add tests for new functionality
5. Run the test suite (`./gradlew test`)
6. Submit a pull request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Write comprehensive documentation for public APIs
- Maintain clean architecture principles

## Support

If you encounter any issues or have questions:

1. Check the [Issues](https://github.com/shekohex/WhisperTop/issues) page for existing reports
2. Create a new issue with detailed information about your problem
3. Include device information, Android version, and error logs when applicable

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Acknowledgments

- OpenAI for providing the Whisper transcription API
- The Kotlin Multiplatform team for the excellent cross-platform framework
- Jetpack Compose team for the modern Android UI toolkit

---

**WhisperTop** - Making voice input available everywhere on Android.