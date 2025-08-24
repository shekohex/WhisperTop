# Module WhisperTop Shared

WhisperTop shared module provides cross-platform business logic, data layer implementations, and domain models for the WhisperTop speech-to-text application.

## Architecture

The shared module follows **Clean Architecture** principles with clear separation between:

- **Domain Layer**: Business logic, use cases, and domain models
- **Data Layer**: Repository implementations, API clients, and data sources
- **Platform Layer**: Platform-specific implementations using expect/actual pattern

## Key Components

### Domain Layer (`domain/`)

- **Use Cases**: Business logic operations (`StartRecordingUseCase`, `StopRecordingUseCase`)
- **Models**: Core domain entities (`AppSettings`, `AudioFile`, `TranscriptionRequest`)
- **Repository Interfaces**: Data access contracts
- **Services**: Abstract service interfaces

### Data Layer (`data/`)

- **Repository Implementations**: Concrete implementations of domain interfaces
- **Remote Data Sources**: OpenAI API client and network communication
- **Local Data Sources**: Database access, preferences, and local storage
- **Models**: DTOs and database entities

### Platform Implementations

- **Android** (`androidMain/`): Android-specific implementations
- **iOS** (`iosMain/`): iOS-specific implementations (future support)

## Usage

This module is designed to be consumed by platform-specific applications while providing shared business logic and data management capabilities.

### Key Features

- ✅ **Cross-platform**: Kotlin Multiplatform with shared business logic
- ✅ **Clean Architecture**: Clear separation of concerns
- ✅ **Dependency Injection**: Koin-based DI for testability
- ✅ **Type Safety**: Comprehensive use of sealed classes and type-safe APIs
- ✅ **Coroutines**: Async programming with Kotlin Coroutines
- ✅ **Serialization**: kotlinx.serialization for JSON handling
- ✅ **Database**: Room database with SQLCipher encryption
- ✅ **Networking**: Ktor client for HTTP communication

## Platform Support

| Platform | Status | Implementation |
|----------|---------|---------------|
| Android  | ✅ Complete | Native Android APIs |
| iOS      | 🔄 Planned | Native iOS APIs |
| Desktop  | 🔄 Future | JVM implementation |

## Documentation

- [Architecture Overview](../../docs/architecture/ARCHITECTURE_OVERVIEW.md)
- [Database Schema](../../docs/DATABASE_SCHEMA.md) 
- [KDoc Standards](../../docs/KDOC_STANDARDS.md)

## Getting Started

```kotlin
// Initialize Koin modules
startKoin {
    modules(sharedModule, androidModule)
}

// Use cases example
class AudioRecordingViewModel(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase
) {
    fun startRecording() = viewModelScope.launch {
        startRecordingUseCase.execute()
    }
}
```

## Testing

The shared module includes comprehensive test coverage:

- **Unit Tests**: Business logic and use case testing
- **Integration Tests**: Repository and API testing  
- **Platform Tests**: Platform-specific implementation testing

Run tests:
```bash
./gradlew shared:test
./gradlew shared:testDebugUnitTest
```

## API Documentation

This documentation is generated from KDoc comments in the source code. For implementation details, see the individual class and interface documentation below.