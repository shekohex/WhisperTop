# API Documentation

## Overview

WhisperTop provides a comprehensive API for speech-to-text transcription with OpenAI integration. This documentation covers all public interfaces, use cases, repositories, and data models.

## Documentation Generation

The API documentation is generated using **Dokka** from KDoc comments in the source code.

### Generate Documentation

```bash
# Generate HTML documentation for all modules
./gradlew dokkaHtml

# Generate documentation for shared module only
./gradlew shared:dokkaHtml

# Generate documentation for Android app module
./gradlew composeApp:dokkaHtml
```

Generated documentation will be available at:
- **Shared Module**: `shared/build/dokka/html/index.html`
- **Android Module**: `composeApp/build/dokka/html/index.html`

## API Structure

### Shared Module APIs

The shared module provides cross-platform APIs organized into layers:

#### 1. Domain Layer APIs

**Use Cases** - Business logic operations:
- [`StartRecordingUseCase`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/usecases/StartRecordingUseCase.kt) - Initiates audio recording
- [`StopRecordingUseCase`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/usecases/StopRecordingUseCase.kt) - Stops recording and processes audio
- [`ApiKeyUseCase`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/usecases/ApiKeyUseCase.kt) - Manages OpenAI API key operations

**Domain Models** - Core data structures:
- [`AppSettings`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/models/AppSettings.kt) - Application configuration
- [`AudioFile`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/models/AudioFile.kt) - Audio file representation
- [`RecordingState`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/models/RecordingState.kt) - Recording state machine
- [`TranscriptionRequest/Response`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/models/) - API communication models

**Repository Interfaces** - Data access contracts:
- [`AudioRepository`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/repositories/AudioRepository.kt)
- [`SettingsRepository`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/repositories/SettingsRepository.kt)
- [`TranscriptionRepository`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/domain/repositories/TranscriptionRepository.kt)

#### 2. Data Layer APIs

**Repository Implementations**:
- [`AudioRepositoryImpl`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/data/repositories/AudioRepositoryImpl.kt)
- [`SettingsRepositoryImpl`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/data/repositories/SettingsRepositoryImpl.kt)
- [`TranscriptionRepositoryImpl`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/data/repositories/TranscriptionRepositoryImpl.kt)

**Remote Data Sources**:
- [`OpenAIApiService`](../shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/data/remote/OpenAIApiService.kt) - OpenAI API client

**Local Data Sources** (Android):
- Database DAOs for local persistence
- Encrypted preferences for settings storage

### Android Module APIs

**ViewModels** - Presentation layer:
- [`AudioRecordingViewModel`](../composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/viewmodels/AudioRecordingViewModel.kt)
- [`SettingsViewModel`](../composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/presentation/viewmodels/SettingsViewModel.kt)

**Services** - Android background services:
- [`AudioRecordingService`](../composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/service/AudioRecordingService.kt)
- [`OverlayService`](../composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/service/OverlayService.kt)
- [`WhisperTopAccessibilityService`](../composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/service/WhisperTopAccessibilityService.kt)

## Core API Examples

### Basic Usage

#### 1. Recording Audio

```kotlin
class AudioRecordingViewModel(
    private val startRecordingUseCase: StartRecordingUseCase,
    private val stopRecordingUseCase: StopRecordingUseCase
) : ViewModel() {
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    fun startRecording() {
        viewModelScope.launch {
            try {
                val result = startRecordingUseCase.execute()
                _recordingState.value = RecordingState.Recording(
                    startTime = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error(e)
            }
        }
    }
    
    fun stopRecording() {
        viewModelScope.launch {
            try {
                _recordingState.value = RecordingState.Processing()
                val result = stopRecordingUseCase.execute()
                _recordingState.value = RecordingState.Success(
                    audioFile = result.audioFile,
                    transcription = result.transcription
                )
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error(e)
            }
        }
    }
}
```

#### 2. Settings Management

```kotlin
class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    
    private val _settings = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = _settings.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.getAppSettings().collect { settings ->
                _settings.value = settings
            }
        }
    }
    
    fun updateApiKey(apiKey: String) {
        viewModelScope.launch {
            val currentSettings = _settings.value ?: return@launch
            val updatedSettings = currentSettings.copy(apiKey = apiKey)
            settingsRepository.updateAppSettings(updatedSettings)
        }
    }
    
    fun updateSelectedModel(model: String) {
        viewModelScope.launch {
            val currentSettings = _settings.value ?: return@launch
            val updatedSettings = currentSettings.copy(selectedModel = model)
            settingsRepository.updateAppSettings(updatedSettings)
        }
    }
}
```

#### 3. Direct API Usage

```kotlin
// Create OpenAI API service
val apiService = createOpenAIApiService(
    apiKey = "sk-your-api-key-here",
    baseUrl = "https://api.openai.com/v1/",
    logLevel = OpenAILogLevel.BASIC
)

// Transcribe audio
suspend fun transcribeAudio(audioData: ByteArray): String {
    val response = apiService.transcribe(
        audioData = audioData,
        fileName = "recording.wav",
        model = WhisperModel.WHISPER_1,
        language = "en",
        temperature = 0.0f
    )
    return response.text
}
```

## API Reference by Category

### 1. Audio Processing

| Interface | Description | Platform |
|-----------|-------------|----------|
| `AudioRecorder` | Audio recording interface | expect/actual |
| `AudioRepository` | Audio data management | shared |
| `StartRecordingUseCase` | Begin recording workflow | shared |
| `StopRecordingUseCase` | Complete recording workflow | shared |

### 2. OpenAI Integration

| Interface | Description | Platform |
|-----------|-------------|----------|
| `OpenAIApiService` | OpenAI API client | shared |
| `TranscriptionRepository` | Transcription data management | shared |
| `CreateTranscriptionRequestDto` | API request model | shared |
| `CreateTranscriptionResponseDto` | API response model | shared |

### 3. Settings and Configuration

| Interface | Description | Platform |
|-----------|-------------|----------|
| `AppSettings` | Application configuration | shared |
| `SettingsRepository` | Settings persistence | shared |
| `PreferenceMigrationHelper` | Settings version migration | shared |
| `DatabaseKeyManager` | Encryption key management | Android |

### 4. Database and Persistence

| Interface | Description | Platform |
|-----------|-------------|----------|
| `AppDatabase` | Room database | Android |
| `TranscriptionHistoryDao` | Transcription history access | Android |
| `SessionMetricsDao` | Session analytics access | Android |
| `UserStatisticsDao` | User statistics access | Android |

### 5. UI and Presentation (Android)

| Interface | Description | Platform |
|-----------|-------------|----------|
| `AudioRecordingViewModel` | Recording state management | Android |
| `SettingsViewModel` | Settings UI state | Android |
| `MicButtonOverlay` | Floating microphone button | Android |
| `OverlayService` | System overlay management | Android |

## Error Handling

### Error Types

```kotlin
// Common error types in the API
sealed class WhisperTopError : Exception() {
    
    // Network errors
    data class NetworkError(
        override val message: String,
        val statusCode: Int? = null
    ) : WhisperTopError()
    
    // API errors
    data class ApiError(
        override val message: String,
        val errorCode: String? = null
    ) : WhisperTopError()
    
    // Permission errors
    data class PermissionError(
        val permission: String,
        override val message: String
    ) : WhisperTopError()
    
    // Audio processing errors
    data class AudioError(
        override val message: String,
        val audioIssue: AudioIssueType
    ) : WhisperTopError()
}

enum class AudioIssueType {
    RECORDING_FAILED,
    INVALID_FORMAT,
    FILE_TOO_LARGE,
    PROCESSING_ERROR
}
```

### Error Recovery

```kotlin
// Example error handling in use cases
class StopRecordingUseCase(
    private val audioRepository: AudioRepository,
    private val transcriptionRepository: TranscriptionRepository
) {
    suspend fun execute(): Result<TranscriptionResult> = try {
        val audioFile = audioRepository.stopRecording()
        val transcription = transcriptionRepository.transcribe(audioFile)
        Result.success(TranscriptionResult(audioFile, transcription))
    } catch (e: WhisperTopError) {
        when (e) {
            is WhisperTopError.NetworkError -> {
                // Retry logic or offline fallback
                Result.failure(e)
            }
            is WhisperTopError.ApiError -> {
                // API-specific error handling
                Result.failure(e)
            }
            else -> Result.failure(e)
        }
    }
}
```

## Testing APIs

### Test Utilities

```kotlin
// Mock implementations for testing
class MockAudioRepository : AudioRepository {
    override suspend fun startRecording(): Result<Unit> = Result.success(Unit)
    override suspend fun stopRecording(): Result<AudioFile> = 
        Result.success(AudioFile.mock())
}

class MockOpenAIApiService : OpenAIApiService {
    override suspend fun transcribe(
        audioData: ByteArray,
        fileName: String,
        model: WhisperModel,
        language: String?,
        prompt: String?,
        responseFormat: AudioResponseFormat,
        temperature: Float
    ): CreateTranscriptionResponseDto = CreateTranscriptionResponseDto(
        text = "Mock transcription"
    )
}
```

### Test Extensions

```kotlin
// Test extensions for easier testing
fun AppSettings.withTestApiKey() = copy(apiKey = "sk-test-key")

fun AudioFile.Companion.mock() = AudioFile(
    path = "/mock/path/audio.wav",
    duration = 5.0f,
    sizeBytes = 1024,
    format = AudioFormat.WAV
)

suspend fun TestScope.runRecordingWorkflow(
    startUseCase: StartRecordingUseCase,
    stopUseCase: StopRecordingUseCase
): TranscriptionResult {
    startUseCase.execute().getOrThrow()
    delay(100) // Simulate recording time
    return stopUseCase.execute().getOrThrow()
}
```

## API Versioning and Compatibility

### Version Management

WhisperTop APIs follow semantic versioning:

- **Major version**: Breaking API changes
- **Minor version**: New features, backward compatible
- **Patch version**: Bug fixes, backward compatible

### Migration Guide

When APIs change, migration guides are provided:

```kotlin
// Example: Migrating from v1 to v2
// v1 (deprecated)
@Deprecated("Use transcribeWithConfig instead", ReplaceWith("transcribeWithConfig(TranscriptionConfig(audioData, fileName))"))
suspend fun transcribe(audioData: ByteArray, fileName: String): String

// v2 (current)
suspend fun transcribeWithConfig(config: TranscriptionConfig): TranscriptionResult
```

## Performance Considerations

### Async APIs

All potentially long-running operations are suspending functions:

```kotlin
// Good: Non-blocking API calls
suspend fun transcribe(audioData: ByteArray): TranscriptionResult

// Avoid: Blocking APIs
fun transcribeSync(audioData: ByteArray): TranscriptionResult // Don't do this
```

### Memory Management

```kotlin
// Use ByteArray for large audio data efficiently
interface AudioRepository {
    suspend fun getAudioData(): ByteArray // Efficient for large files
    suspend fun processAudioStream(): Flow<AudioChunk> // For streaming
}
```

### Caching

```kotlin
// Repository implementations include caching
class SettingsRepositoryImpl(
    private val localDataSource: PreferencesDataSource,
    private val cache: SettingsCache
) : SettingsRepository {
    
    override fun getAppSettings(): Flow<AppSettings> = 
        cache.getOrFetch { localDataSource.getSettings() }
}
```

## Conclusion

WhisperTop's API is designed for:

- **Type Safety**: Comprehensive use of sealed classes and Result types
- **Async Programming**: Coroutines-first design with suspending functions  
- **Testability**: Dependency injection and interface-based design
- **Cross-platform**: Kotlin Multiplatform with shared business logic
- **Performance**: Optimized for mobile device constraints
- **Error Handling**: Comprehensive error types and recovery patterns

For detailed implementation examples and advanced usage, see the generated Dokka documentation and individual KDoc comments in the source code.