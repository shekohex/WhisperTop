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
<!-- TASKMASTER_EXPORT_START -->
> 🎯 **Taskmaster Export** - 2025-08-18 19:48:02 UTC
> 📋 Export: without subtasks • Status filter: none
> 🔗 Powered by [Task Master](https://task-master.dev?utm_source=github-readme&utm_medium=readme-export&utm_campaign=whispertop&utm_content=task-export-link)

| Project Dashboard |  |
| :-                |:-|
| Task Progress     | ███████████░░░░░░░░░ 55% |
| Done | 35 |
| In Progress | 0 |
| Pending | 29 |
| Deferred | 0 |
| Cancelled | 0 |
|-|-|
| Subtask Progress | ████████████████████ 100% |
| Completed | 85 |
| In Progress | 0 |
| Pending | 0 |


| ID | Title | Status | Priority | Dependencies | Complexity |
| :- | :-    | :-     | :-       | :-           | :-         |
| 1 | Update Project Dependencies and Build Configuration | ✓&nbsp;done | high | None | N/A |
| 2 | Configure Android Manifest Permissions and Services | ✓&nbsp;done | high | 1 | N/A |
| 3 | Set up Project Architecture and Package Structure | ✓&nbsp;done | medium | 1 | N/A |
| 4 | Implement Secure API Key Storage | ✓&nbsp;done | high | 1, 3 | N/A |
| 5 | Create Audio Recording Service with WAV Generation | ✓&nbsp;done | high | 2, 3 | N/A |
| 6 | Build OpenAI API Client | ✓&nbsp;done | high | 1, 4 | N/A |
| 7 | Implement Overlay Service Infrastructure | ✓&nbsp;done | high | 2, 3 | N/A |
| 8 | Set up Accessibility Service for Text Insertion | ✓&nbsp;done | high | 2, 3 | N/A |
| 9 | Create Floating Microphone Button UI | ✓&nbsp;done | medium | 7 | N/A |
| 10 | Implement Recording State Management | ✓&nbsp;done | medium | 5, 9 | N/A |
| 11 | Integrate Speech-to-Text Workflow | ✓&nbsp;done | high | 6, 8, 10 | N/A |
| 12 | Create Settings Screen Infrastructure | ✓&nbsp;done | medium | 3 | N/A |
| 13 | Implement API Key Configuration UI | ✓&nbsp;done | medium | 4, 12 | N/A |
| 14 | Build Model Selection Interface | ✓&nbsp;done | medium | 12, 6 | N/A |
| 15 | Implement Language Detection and Selection | ✓&nbsp;done | medium | 6, 12 | N/A |
| 16 | Create Theme Customization System | ✓&nbsp;done | low | 12 | N/A |
| 17 | Add Privacy Controls and Data Management | ✓&nbsp;done | medium | 12, 4 | N/A |
| 18 | Implement Error Handling and User Feedback | ✓&nbsp;done | high | 11 | N/A |
| 19 | Add Quick Settings Tile | ✓&nbsp;done | medium | 10, 2 | N/A |
| 20 | Implement Recording Animations and Feedback | ✓&nbsp;done | medium | 9, 10 | N/A |
| 21 | Add Battery Optimization Handling | ✓&nbsp;done | medium | 7, 5 | N/A |
| 22 | Implement Audio Quality Management | ✓&nbsp;done | medium | 5 | N/A |
| 23 | Add Comprehensive Logging and Debugging | ✓&nbsp;done | low | 11 | N/A |
| 24 | Create Comprehensive Test Suite | ✓&nbsp;done | medium | 11, 18 | N/A |
| 25 | Implement Production Readiness and Optimization | ✓&nbsp;done | high | 24 | N/A |
| 26 | Fix iOS Platform Build Issues and AudioRecorder Implementation | ✓&nbsp;done | low | 5, 3 | N/A |
| 27 | Fix API Key Input Field Validation Bug | ✓&nbsp;done | high | None | N/A |
| 28 | Create Comprehensive Permission Request System with Onboarding Flow | ✓&nbsp;done | high | 2, 3, 7, 8, 12 | N/A |
| 29 | Initialize and Display Floating Mic Button Overlay | ✓&nbsp;done | high | 7, 9, 10 | N/A |
| 30 | Implement Overlay Notification System | ✓&nbsp;done | low | 7, 20, 10 | N/A |
| 31 | Add Support for Custom OpenAI-Compatible Endpoints | ✓&nbsp;done | high | 6, 12, 27 | N/A |
| 32 | Fix Background Transcription Service Reliability | ✓&nbsp;done | high | 5, 21, 29 | N/A |
| 33 | Fix Audio Recording Quality and Sensitivity Issues | ✓&nbsp;done | high | 5, 22 | N/A |
| 34 | Fix Accessibility Service Text Insertion | ✓&nbsp;done | high | 8, 11, 32, 33 | N/A |
| 35 | Add Custom Prompt and Temperature Settings for Transcription | ✓&nbsp;done | high | 12, 31 | N/A |
| 36 | Refactor AudioRecordingViewModel to Remove Business Logic and Use TranscriptionWorkflowUseCase | ○&nbsp;pending | high | 3, 5, 32, 33, 34 | N/A |
| 37 | Fix AudioRecordingViewModel Dependency Injection Violations | ○&nbsp;pending | high | 5, 8, 18 | N/A |
| 38 | Create Service Management Use Cases - ServiceInitializationUseCase, PermissionManagementUseCase, and ServiceBindingUseCase | ○&nbsp;pending | high | 5, 7, 8, 10, 11 | N/A |
| 39 | Eliminate Toast Logic from AudioRecordingViewModel | ○&nbsp;pending | high | 37, 8, 6 | N/A |
| 40 | Refactor AudioServiceManager to Proper Abstraction with Clean Architecture Compliance | ○&nbsp;pending | medium | 5, 18, 32 | N/A |
| 41 | Refactor ViewModel State Management - Separate UI State from Domain State | ○&nbsp;pending | medium | 3, 10 | N/A |
| 42 | Implement Proper Error Handling Abstraction with ErrorMapper | ○&nbsp;pending | medium | 10, 13, 23 | N/A |
| 43 | Remove KoinComponent from AudioRecordingViewModel and Implement Constructor Injection | ○&nbsp;pending | high | 5, 37 | N/A |
| 44 | Create DurationTrackerUseCase to Eliminate Coroutine Management from ViewModel | ○&nbsp;pending | medium | 10, 39, 6 | N/A |
| 45 | Create Integration Tests for Clean Architecture Compliance | ○&nbsp;pending | low | 10, 18, 38 | N/A |
| 46 | Set up Room Database Infrastructure for Transcription History Storage | ○&nbsp;pending | high | 1 | N/A |
| 47 | Create Data Models and Domain Entities for Statistics Tracking | ○&nbsp;pending | high | 3 | N/A |
| 48 | Implement DashboardViewModel for Statistics Management | ○&nbsp;pending | high | 10, 13, 42 | N/A |
| 49 | Build Enhanced Dashboard UI with Statistics Display | ○&nbsp;pending | high | 48, 16, 7 | N/A |
| 50 | Implement 30-day usage trend chart component | ○&nbsp;pending | medium | 48, 16, 42 | N/A |
| 51 | Create HistoryViewModel with Pagination and Search Functionality | ○&nbsp;pending | high | 3, 10, 18 | N/A |
| 52 | Build Transcription History Screen with Search and List UI | ○&nbsp;pending | high | 3, 51 | N/A |
| 53 | Implement transcription detail view and actions | ○&nbsp;pending | medium | 48, 16, 42 | N/A |
| 54 | Create PermissionsViewModel for permission state management | ○&nbsp;pending | high | 3, 7 | N/A |
| 55 | Build Permissions Dashboard UI Screen | ○&nbsp;pending | high | 54, 12 | N/A |
| 56 | Implement WPM Configuration and Onboarding Flow | ○&nbsp;pending | medium | 13, 16, 48 | N/A |
| 57 | Update navigation architecture with bottom navigation | ○&nbsp;pending | medium | 3, 16 | N/A |
| 58 | Integrate Statistics Tracking with Existing Recording Services | ○&nbsp;pending | high | 11, 46, 47, 5, 7, 8 | N/A |
| 59 | Implement data export and retention policies | ○&nbsp;pending | medium | 10, 13, 42, 48 | N/A |
| 60 | Implement Performance Optimization and Caching Layer | ○&nbsp;pending | medium | 48, 10, 42, 13 | N/A |
| 61 | Create Comprehensive Test Suite for New Features | ○&nbsp;pending | high | 3, 10, 12, 16, 18 | N/A |
| 62 | Implement UI Polish and Animations | ○&nbsp;pending | low | 16, 20, 12 | N/A |
| 63 | Implement User Preferences and Settings for Statistics Features | ○&nbsp;pending | low | 12, 17, 48 | N/A |
| 64 | Create Comprehensive Documentation and Migration Guide | ○&nbsp;pending | low | 3, 7, 9, 10, 12, 14, 16, 18, 20 | N/A |

> 📋 **End of Taskmaster Export** - Tasks are synced from your project using the `sync-readme` command.
<!-- TASKMASTER_EXPORT_END -->

