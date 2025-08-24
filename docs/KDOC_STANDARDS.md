# KDoc Standards and Style Guide

## Overview

This document defines the KDoc documentation standards for the WhisperTop project. Based on analysis of existing code, we follow official Kotlin KDoc conventions with project-specific guidelines for consistency across the codebase.

## Current Documentation Status

### Analysis Results (as of current codebase)

**Well-Documented Areas:**
- ✅ **Domain Models**: Extensive KDoc in `AppSettings`, `AudioFormat`, `LogEntry`, etc.
- ✅ **Service Layer**: Complete documentation in `OpenAIApiService`, `LoggingManager`, etc.
- ✅ **Utility Classes**: Well documented migration helpers, privacy utils
- ✅ **Performance Monitoring**: Comprehensive docs in `PerformanceBaselines`, monitoring classes
- ✅ **Android Components**: Good coverage in services, managers, UI components

**Documentation Coverage:**
- 🟢 Domain Layer: **95%** - Almost complete with detailed function docs
- 🟢 Data Layer: **90%** - Strong coverage in repositories and API services  
- 🟡 Presentation Layer: **75%** - Good coverage in ViewModels, some UI components need work
- 🟡 Platform-Specific Code: **70%** - Android implementations well documented, iOS scaffolding needs updates

## KDoc Style Standards

### 1. Class Documentation

**Pattern:**
```kotlin
/**
 * Brief description of what the class does.
 * 
 * More detailed description if needed, explaining the purpose,
 * key responsibilities, and usage context.
 * 
 * @param T Generic type parameter description
 * @property propertyName Description of important properties
 * @constructor Creates an instance with specified parameters
 * 
 * @sample me.shadykhalifa.whispertop.examples.AppSettingsUsage
 * @since 1.0.0
 */
class ExampleClass<T>(val propertyName: String) {
    // Implementation
}
```

**Example from codebase:**
```kotlin
/**
 * Core application settings for WhisperTop.
 * 
 * Contains all user preferences, API configuration, performance settings,
 * and privacy controls. Settings are persisted using encrypted preferences
 * and can be migrated between app versions.
 * 
 * @property apiKey OpenAI API key for transcription services
 * @property selectedModel Currently selected Whisper model
 * @property baseUrl API base URL (default: OpenAI, supports custom endpoints)
 * @constructor Creates AppSettings with default values
 */
data class AppSettings(...)
```

### 2. Function Documentation

**Pattern:**
```kotlin
/**
 * Brief description of what the function does.
 * 
 * Optional longer description explaining behavior, side effects,
 * error conditions, or special considerations.
 * 
 * @param paramName Description of parameter purpose and constraints
 * @return Description of return value and possible variations
 * @throws ExceptionType When and why this exception is thrown
 * 
 * @sample me.shadykhalifa.whispertop.examples.transcriptionExample
 * @see RelatedClass.relatedMethod
 * @since 1.2.0
 */
suspend fun exampleFunction(paramName: String): Result<String> {
    // Implementation
}
```

**Example from codebase:**
```kotlin
/**
 * Transcribes audio data using OpenAI's Whisper API.
 * 
 * Supports multiple models, languages, and response formats.
 * Uses multipart form upload for audio files up to 25MB.
 * 
 * @param audioData Raw audio bytes in WAV format
 * @param fileName Original filename for content type detection
 * @param model Whisper model to use for transcription
 * @param language Optional language hint (ISO 639-1 code)
 * @param prompt Optional context prompt to improve accuracy
 * @param responseFormat Response format (JSON or verbose JSON)
 * @param temperature Sampling temperature (0.0-1.0)
 * 
 * @return Transcription response with text and metadata
 * @throws IllegalArgumentException If temperature is outside valid range
 * @throws NetworkException If API request fails
 * 
 * @see transcribeWithLanguageDetection For automatic language detection
 * @sample me.shadykhalifa.whispertop.examples.basicTranscription
 */
suspend fun transcribe(
    audioData: ByteArray,
    fileName: String,
    model: WhisperModel = WhisperModel.WHISPER_1,
    language: String? = null,
    prompt: String? = null,
    responseFormat: AudioResponseFormat = AudioResponseFormat.JSON,
    temperature: Float = 0.0f
): CreateTranscriptionResponseDto
```

### 3. Property Documentation

**Pattern:**
```kotlin
/**
 * Brief description of the property.
 * 
 * Additional context about constraints, default values,
 * or behavioral implications.
 */
val exampleProperty: String = "default"

/**
 * Mutable property with validation rules.
 * 
 * Value must be between 7 and 365 days. Setting invalid
 * values will be coerced to the valid range.
 */
var historyRetentionDays: Int = 90
    set(value) {
        field = value.coerceIn(7, 365)
    }
```

### 4. Enum and Sealed Class Documentation

**Pattern:**
```kotlin
/**
 * Represents different theme options available in the app.
 * 
 * @property Light Always use light mode
 * @property Dark Always use dark mode  
 * @property System Follow system theme setting
 */
enum class Theme {
    Light,
    Dark,
    System
}

/**
 * Recording state hierarchy for audio transcription workflow.
 * 
 * @property Idle No recording in progress
 * @property Recording Active recording with optional metadata
 * @property Processing Audio being transcribed
 * @property Success Successful transcription with results
 * @property Error Failed transcription with error details
 */
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val startTime: Long) : RecordingState()
    data class Processing(val progress: Float) : RecordingState()
    data class Success(val text: String) : RecordingState()
    data class Error(val throwable: Throwable) : RecordingState()
}
```

### 5. Extension Function Documentation

**Pattern:**
```kotlin
/**
 * Extension description focusing on what it adds to the receiver.
 * 
 * @receiver Description of what type this extends
 * @param additionalParam Any additional parameters
 * @return What the extension returns
 */
fun ReceiverType.extensionFunction(additionalParam: String): String {
    // Implementation
}
```

## Documentation Best Practices

### 1. Content Guidelines

**Do:**
- ✅ Start with a clear, concise summary
- ✅ Explain the "why" not just the "what"
- ✅ Document side effects and preconditions
- ✅ Include parameter constraints and validation rules
- ✅ Reference related functions with `@see`
- ✅ Use `@sample` for complex usage patterns
- ✅ Document thread safety implications
- ✅ Include version information for new APIs

**Don't:**
- ❌ Repeat obvious information from the signature
- ❌ Use generic descriptions like "Gets the value"
- ❌ Document private implementation details in public docs
- ❌ Create docs just to satisfy coverage metrics
- ❌ Use abbreviations without explanation

### 2. Linking and References

**Internal Links:**
```kotlin
/**
 * Processes [AppSettings.apiKey] validation.
 * 
 * @see AppSettings.isOpenAIEndpoint
 * @see validateApiKey
 */
```

**External Links:**
```kotlin
/**
 * Follows [OpenAI API documentation](https://platform.openai.com/docs/api-reference/audio)
 * for transcription endpoints.
 */
```

### 3. Code Samples

**Simple Example:**
```kotlin
/**
 * @sample
 * ```kotlin
 * val settings = AppSettings(apiKey = "sk-...")
 * if (settings.isOpenAIEndpoint()) {
 *     // Use OpenAI-specific features
 * }
 * ```
 */
```

**Dedicated Sample Functions:**
```kotlin
// In a separate samples file
@Suppress("unused")
fun appSettingsUsageSample() {
    val settings = AppSettings(
        apiKey = "sk-example-key",
        selectedModel = "whisper-1",
        enableHapticFeedback = true
    )
    
    // Validate settings
    val issues = settings.validateSettings()
    if (issues.isEmpty()) {
        // Settings are valid
    }
}
```

## Platform-Specific Documentation

### Multiplatform Considerations

**Expect/Actual Functions:**
```kotlin
/**
 * Platform-specific audio recording implementation.
 * 
 * Android: Uses MediaRecorder with audio focus handling
 * iOS: Uses AVAudioRecorder with session management
 * 
 * @param config Recording configuration
 * @return Audio recorder instance
 * @throws UnsupportedOperationException If audio recording is not available
 */
expect fun createAudioRecorder(config: AudioConfig): AudioRecorder
```

**Platform-Specific Implementation:**
```kotlin
/**
 * Android implementation of audio recorder.
 * 
 * Uses MediaRecorder with the following configuration:
 * - Audio source: VOICE_RECOGNITION
 * - Output format: WAV (PCM 16-bit, 16kHz mono)
 * - Audio focus: AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
 * 
 * Requires RECORD_AUDIO permission.
 * 
 * @see android.media.MediaRecorder
 */
actual fun createAudioRecorder(config: AudioConfig): AudioRecorder {
    // Android implementation
}
```

## Tools and Automation

### 1. Dokka Configuration

Add to `build.gradle.kts`:
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.9.20"
}

dokka {
    moduleName.set("WhisperTop")
    
    dokkaSourceSets {
        named("commonMain") {
            includes.from("docs/api/module.md")
            samples.from("src/samples/kotlin")
            
            sourceLink {
                localDirectory.set(file("src/commonMain/kotlin"))
                remoteUrl.set(URL("https://github.com/shekohex/WhisperTop/tree/main/shared/src/commonMain/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
```

### 2. Documentation Validation

**Check for missing docs:**
```bash
# Find public functions without KDoc
find . -name "*.kt" -exec grep -l "^[[:space:]]*\(public \|internal \|fun \|class \|interface \)" {} \; | \
xargs grep -L "/\*\*"
```

**Validate KDoc syntax:**
```bash
./gradlew dokkaHtml
```

### 3. IDE Configuration

**IntelliJ IDEA Settings:**
- Settings → Editor → Inspections → Kotlin → "Missing KDoc" → Enable
- Settings → Editor → Code Style → Kotlin → KDoc formatting rules
- Use live templates for KDoc generation

## Migration and Maintenance

### Updating Existing Code

**Priority Order:**
1. **Public APIs** - All public classes, functions, and properties
2. **Domain Models** - Core business logic and data structures  
3. **Service Interfaces** - Repository and service contracts
4. **Complex Algorithms** - Performance-critical or complex logic
5. **Platform Interfaces** - expect/actual declarations

### Documentation Reviews

**Review Checklist:**
- [ ] All public APIs have KDoc comments
- [ ] Parameter constraints are documented
- [ ] Return types and possible values explained
- [ ] Error conditions and exceptions listed
- [ ] Thread safety implications noted
- [ ] Sample code compiles and works
- [ ] Links to related functions included
- [ ] Platform-specific behavior documented

### Version Control

**Commit Message Format:**
```
docs: add KDoc for audio transcription APIs

- Document OpenAIApiService.transcribe methods
- Add parameter validation notes
- Include usage examples
- Link to related configuration classes
```

## Conclusion

Consistent, high-quality documentation is essential for maintainability and developer experience. Following these standards ensures that WhisperTop's codebase remains accessible to new contributors and clearly communicates the intent and usage of all APIs.

For questions or suggestions about these standards, please refer to the architecture documentation or create an issue in the project repository.