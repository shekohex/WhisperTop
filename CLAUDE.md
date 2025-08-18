# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

WhisperTop is an Android overlay application that enables quick and accurate speech-to-text transcription anywhere on the device using OpenAI's Whisper API. It features a floating microphone button that works on top of any app, allowing users to transcribe speech directly into text fields using their own OpenAI API key.

## Development Commands

### Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK  
./gradlew assembleRelease

# Build debug AAB (Android App Bundle)
./gradlew bundleDebug

# Build release AAB
./gradlew bundleRelease

# Install debug APK on connected device
./gradlew installDebug

# Clean build
./gradlew clean
```

### Testing Commands

```bash
# Run all unit tests
./gradlew test

# Run Android unit tests only
./gradlew testDebugUnitTest

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedDebugAndroidTest

# Run specific test class
./gradlew testDebugUnitTest --tests "me.shadykhalifa.whispertop.presentation.AudioRecordingViewModelTest"

# Run lint checks
./gradlew lint

# Run lint with auto-fix
./gradlew lintFix

# Run all checks (lint + tests)
./gradlew check
```

### Development Workflow

```bash
# Check dependencies
./gradlew androidDependencies

# Generate signing report
./gradlew signingReport

# View available tasks
./gradlew tasks
```

IMPORTANT: When you do git commit, use the git-commiter agent and ask it to commit without gpg signing, otherwise the commit will fail.

## Architecture Overview

### Kotlin Multiplatform Structure

- **`composeApp/`** - Android-specific UI and platform features
- **`shared/`** - Cross-platform business logic and data layer
- **`iosApp/`** - iOS entry point (future iOS support)

### Clean Architecture Layers

#### Domain Layer (`shared/src/commonMain/kotlin/.../domain/`)

- **Models**: Core data structures (`AppSettings`, `AudioFile`, `TranscriptionRequest/Response`, `RecordingState`)
- **Repositories**: Abstract interfaces for data access
- **Use Cases**: Business logic operations (`StartRecordingUseCase`, `StopRecordingUseCase`, `ApiKeyUseCase`)
- **Services**: Platform-agnostic service interfaces

#### Data Layer (`shared/src/commonMain/kotlin/.../data/`)

- **Repositories**: Concrete repository implementations
- **Remote**: OpenAI API client with Ktor HTTP client
- **Local**: Secure preferences and data storage
- **Audio**: Audio recording and WAV file management
- **Models**: DTOs for API communication

#### Presentation Layer (`composeApp/src/androidMain/kotlin/.../`)

- **ViewModels**: `AudioRecordingViewModel` for recording state management
- **Services**: Android-specific services (overlay, accessibility, audio recording)
- **Managers**: Service coordination and permission handling

### Key Android Components

#### Services Architecture

1. **`AudioRecordingService`** - Foreground service for microphone capture
2. **`OverlayService`** - System overlay for floating mic button
3. **`WhisperTopAccessibilityService`** - Text insertion via accessibility API

#### Overlay System

- Floating microphone button with drag functionality
- Visual state indicators (idle/recording/processing)
- Window manager integration for system-wide overlay

#### Security & Storage

- **Encrypted Preferences**: API keys stored using `EncryptedSharedPreferences`
- **Secure API Communication**: Direct OpenAI API calls with user's own key
- **Platform-specific**: Android keystore integration

### Dependency Injection (Koin)

- **Shared Module**: Cross-platform dependencies
- **Android Module**: Platform-specific implementations
- **Application Module**: Android app-level dependencies

## Current Development Status

This project uses Task Master AI for development planning. Current status:

- **38% complete** (10/26 tasks done)
- **16 pending tasks** including overlay integration, API workflow, and UI components

### Next Available Tasks

Run `task-master next` to see the next available task, or check pending tasks with `task-master list --status=pending`.

## Testing Strategy

### Unit Tests

- **Location**: `composeApp/src/test/kotlin/` (Android unit tests)
- **Shared Tests**: `shared/src/commonTest/kotlin/` (Multiplatform tests)
- **Frameworks**: JUnit 4, Mockito, Koin Test, Coroutines Test

### Instrumented Tests  

- **Location**: `composeApp/src/androidTest/kotlin/`
- **Focus**: Service integration, overlay functionality, accessibility features
- **Requirements**: Android device/emulator with API 26+

### Test Configurations

- **Mock HTTP Client**: Ktor MockEngine for API testing
- **Koin Test**: Dependency injection testing utilities
- **Coroutines Test**: `TestCoroutineDispatcher` for async testing

## Key Dependencies

### Core Stack

- **Kotlin 2.2.0** with K2 compiler
- **Compose Multiplatform 1.8.2** with Material 3
- **Android Gradle Plugin 8.12.0**
- **Target SDK 35** (Android 15), **Min SDK 26**

### Networking & Serialization

- **Ktor 3.0.3** for HTTP client (OkHttp on Android)
- **Kotlinx Serialization 1.7.3** for JSON handling

### Android-Specific

- **AndroidX Lifecycle 2.9.1** for ViewModels and services
- **AndroidX Security 1.1.0-alpha06** for encrypted storage
- **AndroidX Work 2.9.0** for background tasks
- **AndroidX Navigation 2.8.5** for Compose navigation

### Dependency Injection

- **Koin 4.0.2** for dependency injection across platforms

## Permissions Required

Critical Android permissions for functionality:

- `RECORD_AUDIO` - Microphone access for speech capture
- `SYSTEM_ALERT_WINDOW` - Overlay permission for floating UI
- `BIND_ACCESSIBILITY_SERVICE` - Text insertion capability
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MICROPHONE` - Background recording
- `INTERNET` - OpenAI API communication

## OpenAI Integration

### API Configuration

- **Endpoint**: `/v1/audio/transcriptions`
- **Models**: Whisper-1, GPT-4o (configurable)
- **Format**: Multipart form-data with WAV files (PCM16 mono @16kHz)
- **Authentication**: User's BYO API key

### Audio Processing Pipeline

1. **Capture**: Android MediaRecorder → PCM audio data
2. **Format**: WAV file generation with proper headers
3. **Upload**: Multipart HTTP request to OpenAI
4. **Response**: JSON parsing for transcribed text
5. **Insertion**: Accessibility service for text input

## Development Notes

### Source Directory Migration

The project has deprecated Android-style source directories. Consider migrating:

- `composeApp/src/androidTest/` → `composeApp/src/androidInstrumentedTest/`
- `composeApp/src/test/` → `composeApp/src/androidUnitTest/`

### Platform Considerations

- iOS support is scaffolded but not yet implemented
- Focus development on Android platform features first
- Shared business logic is designed for future iOS compatibility

## Task Master AI Instructions

**Import Task Master's development workflow commands and guidelines, treat as if import is in the main CLAUDE.md file.**
@./.taskmaster/CLAUDE.md
