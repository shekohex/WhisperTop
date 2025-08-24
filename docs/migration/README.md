# Migration Guide

This document provides comprehensive guidance for migrating and extending the WhisperTop project, including iOS support preparation, source directory migrations, and cross-platform considerations.

## Table of Contents

- [Overview](#overview)
- [Source Directory Migration](#source-directory-migration)
- [iOS Support Preparation](#ios-support-preparation)
- [Cross-Platform Considerations](#cross-platform-considerations)
- [Database Migration Strategy](#database-migration-strategy)
- [Architecture Migration](#architecture-migration)
- [Testing Migration](#testing-migration)
- [Future Platform Support](#future-platform-support)

---

## Overview

WhisperTop has evolved from an Android-only application to a **Kotlin Multiplatform** project with **Clean Architecture**. This migration guide covers the steps needed to:

1. **Migrate deprecated source directories** to the new KMP structure
2. **Prepare for iOS support** with shared business logic
3. **Maintain backwards compatibility** while adopting modern patterns
4. **Extend to additional platforms** in the future

---

## Source Directory Migration

### Current State

The project has successfully migrated from deprecated Android-style source directories to the modern Kotlin Multiplatform structure:

#### Old Structure (Deprecated)
```
composeApp/src/
├── androidTest/         # Deprecated
├── test/                # Deprecated
├── main/                # Android main source
└── debug/               # Debug build variant
```

#### New Structure (Current)
```
composeApp/src/
├── androidInstrumentedTest/    # Android instrumented tests
├── androidUnitTest/            # Android unit tests  
├── androidMain/                # Android-specific implementation
├── commonMain/                 # Shared multiplatform code
├── commonTest/                 # Shared test code
└── debug/                      # Debug build variant
```

### Migration Steps (Completed)

The following migrations have been completed in the current project:

1. **✅ Source Set Migration**
   - Moved `src/test/` → `src/androidUnitTest/`
   - Moved `src/androidTest/` → `src/androidInstrumentedTest/`
   - Created `src/commonMain/` for shared code
   - Created `src/commonTest/` for shared tests

2. **✅ Build Configuration Updates**
   ```kotlin
   // build.gradle.kts - Updated source sets
   kotlin {
       sourceSets {
           androidUnitTest.dependencies {
               // Android-specific test dependencies
           }
           androidInstrumentedTest.dependencies {
               // Instrumentation test dependencies
           }
           commonTest.dependencies {
               // Shared test dependencies
           }
       }
   }
   ```

3. **✅ Package Structure**
   - Android-specific: `composeApp/src/androidMain/kotlin/me/shadykhalifa/whispertop/`
   - Shared code: `shared/src/commonMain/kotlin/me/shadykhalifa/whispertop/`
   - Platform abstraction: Expect/actual pattern for platform-specific implementations

---

## iOS Support Preparation

### Current iOS Readiness

The project is **90% ready** for iOS implementation with the following structure:

#### Shared Components (✅ Completed)
```
shared/src/commonMain/kotlin/
├── domain/                     # Business logic (100% ready)
│   ├── models/                 # Data models
│   ├── repositories/           # Repository interfaces
│   ├── services/               # Service interfaces
│   └── usecases/               # Use cases
├── data/                       # Data layer (95% ready)
│   ├── remote/                 # API client (Ktor - cross-platform)
│   ├── local/                  # Preferences abstraction
│   └── repositories/           # Repository implementations
└── di/                         # Dependency injection (Koin)
```

#### iOS-Specific Implementation (⏳ Pending)
```
composeApp/src/iosMain/kotlin/
├── Platform.ios.kt             # iOS platform implementations
├── audio/                      # iOS audio recording (AVAudioRecorder)
├── overlay/                    # iOS overlay system (alternative approach)
├── accessibility/              # iOS accessibility services
└── storage/                    # iOS secure storage (Keychain)
```

### iOS Implementation Steps

#### 1. Audio Recording Implementation
```kotlin
// Expected interface (already defined)
expect class AudioRecorder {
    suspend fun startRecording(outputFile: String)
    suspend fun stopRecording(): String
    fun isRecording(): Boolean
}

// iOS implementation needed
actual class AudioRecorder {
    // AVAudioRecorder implementation
    private var audioRecorder: AVAudioRecorder? = null
    
    actual suspend fun startRecording(outputFile: String) {
        // iOS-specific recording logic
    }
    
    actual suspend fun stopRecording(): String {
        // Return recorded file path
    }
    
    actual fun isRecording(): Boolean {
        return audioRecorder?.isRecording() ?: false
    }
}
```

#### 2. Overlay System Alternative
iOS doesn't support system-wide overlays like Android. Alternative approaches:

1. **Keyboard Extension**: Custom keyboard with speech input
2. **Share Extension**: Speech input from share sheet
3. **Siri Shortcuts**: Voice-activated transcription
4. **Widget**: Home screen widget for quick access

#### 3. Text Insertion Strategy
```kotlin
// iOS text insertion approaches
expect class TextInserter {
    suspend fun insertText(text: String)
}

// iOS implementation options:
// 1. Clipboard + notification
// 2. URL scheme integration
// 3. Share extension workflow
```

### iOS Development Requirements

#### Xcode Configuration
```xml
<!-- Info.plist additions -->
<key>NSMicrophoneUsageDescription</key>
<string>WhisperTop needs microphone access for speech transcription</string>

<key>NSAppTransportSecurity</key>
<dict>
    <key>NSExceptionDomains</key>
    <dict>
        <key>api.openai.com</key>
        <dict>
            <key>NSExceptionRequiresForwardSecrecy</key>
            <false/>
        </dict>
    </dict>
</dict>
```

#### Swift Integration
```swift
// ContentView.swift integration
import ComposeApp

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all)
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return Main_iosKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
```

---

## Cross-Platform Considerations

### Platform Abstraction Strategy

#### 1. Expect/Actual Declarations
```kotlin
// Common code expectation
expect suspend fun getPlatformSpecificData(): String

// Android actual implementation
actual suspend fun getPlatformSpecificData(): String {
    return "Android-specific data"
}

// iOS actual implementation (future)
actual suspend fun getPlatformSpecificData(): String {
    return "iOS-specific data"
}
```

#### 2. Dependency Injection Strategy
```kotlin
// Common module
val commonModule = module {
    single<ApiKeyRepository> { ApiKeyRepositoryImpl(get()) }
    single<TranscriptionRepository> { TranscriptionRepositoryImpl(get()) }
}

// Platform-specific modules
val androidModule = module {
    single<AudioRecorder> { AndroidAudioRecorder() }
    single<TextInserter> { AndroidTextInserter(get()) }
}

val iosModule = module {
    single<AudioRecorder> { IOSAudioRecorder() }
    single<TextInserter> { IOSTextInserter() }
}
```

### Data Sharing Strategy

#### 1. Serialization
```kotlin
@Serializable
data class TranscriptionData(
    val id: String,
    val text: String,
    val timestamp: Long,
    val duration: Int
)
```

#### 2. Database Synchronization
```kotlin
// Room database (Android) ↔ Core Data (iOS)
expect class DatabaseProvider {
    suspend fun saveTranscription(data: TranscriptionData)
    suspend fun getTranscriptions(): List<TranscriptionData>
}
```

---

## Database Migration Strategy

### Current Database State
- **Version 4**: Latest schema with comprehensive statistics tracking
- **Encryption**: SQLCipher for data security
- **Migrations**: Complete migration path from v1→v4

### Cross-Platform Database Options

#### Option 1: SQLDelight (Recommended)
```kotlin
// Shared SQL definitions
CREATE TABLE TranscriptionHistory (
    id TEXT NOT NULL PRIMARY KEY,
    originalText TEXT NOT NULL,
    timestamp INTEGER NOT NULL,
    duration INTEGER NOT NULL
);
```

#### Option 2: Realm Kotlin
```kotlin
class TranscriptionRealm : RealmObject {
    var id: String = ""
    var originalText: String = ""
    var timestamp: Long = 0L
    var duration: Int = 0
}
```

### Migration Script Example
```kotlin
// Database migration utility
object DatabaseMigrator {
    suspend fun migrateToMultiplatform() {
        val androidData = getAndroidRoomData()
        val sharedDatabase = getSharedDatabase()
        
        androidData.forEach { transcription ->
            sharedDatabase.insert(transcription.toSharedModel())
        }
    }
}
```

---

## Architecture Migration

### Clean Architecture Compliance

The project has successfully migrated to Clean Architecture:

#### Domain Layer (✅ Complete)
- Pure Kotlin with no platform dependencies
- Business logic isolated in use cases
- Repository interfaces as contracts

#### Data Layer (✅ Complete)
- Repository implementations with platform abstractions
- Ktor HTTP client for cross-platform networking
- Expect/actual pattern for platform-specific storage

#### Presentation Layer (✅ Android, ⏳ iOS pending)
- ViewModels using shared domain layer
- Platform-specific UI implementations
- Dependency injection with Koin

### Migration Validation Checklist

#### ✅ Completed
- [x] Domain layer completely platform-agnostic
- [x] Use cases independent of platform
- [x] Repository pattern with abstract interfaces
- [x] Dependency injection configured for multiplatform
- [x] API client using cross-platform Ktor
- [x] Serialization with kotlinx.serialization

#### ⏳ iOS Implementation Needed  
- [ ] iOS-specific audio recording implementation
- [ ] iOS text insertion mechanism
- [ ] iOS secure storage (Keychain integration)
- [ ] iOS UI implementation with Compose Multiplatform

---

## Testing Migration

### Current Testing Infrastructure

#### Test Coverage: 83.1%
- **Unit Tests**: 324 tests (Android + Shared)
- **Integration Tests**: 89 tests
- **Instrumented Tests**: 71 tests

#### Cross-Platform Testing Strategy
```kotlin
// Shared tests (commonTest)
class TranscriptionUseCaseTest {
    @Test
    fun testTranscriptionWorkflow() = runTest {
        // Test business logic on all platforms
    }
}

// Platform-specific tests
class AndroidAudioRecorderTest {
    @Test
    fun testAndroidRecording() {
        // Android-specific recording tests
    }
}
```

### Testing Migration Steps

#### 1. Shared Test Infrastructure
```kotlin
// Common test utilities
expect class TestDatabase {
    suspend fun setupTestData()
    suspend fun clearTestData()
}
```

#### 2. Platform Test Configuration
```kotlin
// build.gradle.kts test configuration
kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        
        androidUnitTest.dependencies {
            implementation(libs.junit)
            implementation(libs.mockito.kotlin)
            implementation(libs.robolectric)
        }
        
        iosTest.dependencies {
            // iOS-specific test dependencies
        }
    }
}
```

---

## Future Platform Support

### Desktop Support (JVM)
```kotlin
// Desktop-specific implementations
actual class DesktopAudioRecorder : AudioRecorder {
    // Java Sound API implementation
}

actual class DesktopTextInserter : TextInserter {
    // Robot class for text insertion
}
```

### Web Support (Kotlin/JS)
```kotlin
// Web implementations
actual class WebAudioRecorder : AudioRecorder {
    // Web Audio API implementation
}

actual class WebTextInserter : TextInserter {
    // DOM manipulation for text insertion
}
```

### Platform Support Matrix

| Feature | Android | iOS | Desktop | Web |
|---------|---------|-----|---------|-----|
| Audio Recording | ✅ | ⏳ | 📋 | 📋 |
| Text Insertion | ✅ | ⏳ | 📋 | 📋 |
| API Integration | ✅ | ✅ | ✅ | ✅ |
| Local Storage | ✅ | ⏳ | 📋 | 📋 |
| Database | ✅ | ⏳ | 📋 | 📋 |

**Legend**: ✅ Complete, ⏳ Planned, 📋 Future consideration

---

## Migration Checklist

### For Developers

#### Starting iOS Development
- [ ] Set up Xcode project with Kotlin Multiplatform plugin
- [ ] Implement iOS-specific audio recording (`actual class AudioRecorder`)
- [ ] Create iOS text insertion strategy (clipboard/share extension)
- [ ] Integrate iOS secure storage (Keychain Services)
- [ ] Implement iOS-specific UI layer
- [ ] Test cross-platform business logic on iOS

#### Adding New Platforms
- [ ] Analyze platform-specific capabilities and limitations
- [ ] Create platform module in dependency injection
- [ ] Implement expect/actual declarations for platform features
- [ ] Create platform-specific UI implementation
- [ ] Add platform-specific tests
- [ ] Update CI/CD pipeline for new platform builds

#### Maintaining Cross-Platform Compatibility
- [ ] Keep domain layer pure Kotlin (no platform dependencies)
- [ ] Use expect/actual pattern for platform-specific implementations
- [ ] Maintain consistent API contracts across platforms
- [ ] Write shared tests for business logic
- [ ] Document platform-specific differences and limitations

---

## Additional Resources

- **Architecture Documentation**: [docs/architecture/ARCHITECTURE_OVERVIEW.md](../architecture/ARCHITECTURE_OVERVIEW.md)
- **Database Schema**: [docs/DATABASE_SCHEMA.md](../DATABASE_SCHEMA.md)
- **Testing Strategy**: [docs/testing/ADVANCED_TESTING_STRATEGY.md](../testing/ADVANCED_TESTING_STRATEGY.md)
- **Performance Guidelines**: [docs/performance/PERFORMANCE_GUIDE.md](../performance/PERFORMANCE_GUIDE.md)

For questions about migration strategies or platform-specific implementations, please refer to the main project documentation or create an issue in the repository.