# Architecture Overview

## Introduction

WhisperTop follows a **Clean Architecture** pattern with **Kotlin Multiplatform** support, implementing **MVVM** for the presentation layer. The architecture is designed for scalability, testability, and cross-platform compatibility while maintaining strict separation of concerns.

## High-Level Architecture

```mermaid
graph TB
    subgraph "Presentation Layer (Android)"
        UI[UI Components<br/>Compose UI]
        VM[ViewModels<br/>State Management]
        Services[Android Services<br/>Overlay, Recording, Accessibility]
    end
    
    subgraph "Domain Layer (Shared)"
        UC[Use Cases<br/>Business Logic]
        Models[Domain Models<br/>Core Entities]
        RepoInt[Repository Interfaces<br/>Contracts]
    end
    
    subgraph "Data Layer (Shared)"
        RepoImpl[Repository Implementations<br/>Data Access]
        Remote[Remote Data Sources<br/>OpenAI API]
        Local[Local Data Sources<br/>Database, Preferences]
    end
    
    subgraph "Platform Layer"
        Android[Android Implementations<br/>Storage, Audio, UI]
        iOS[iOS Implementations<br/>Future Support]
    end
    
    UI --> VM
    VM --> UC
    Services --> UC
    UC --> RepoInt
    RepoImpl --> RepoInt
    RepoImpl --> Remote
    RepoImpl --> Local
    Android --> Local
    iOS --> Local
    
    style UI fill:#e1f5fe
    style VM fill:#e8f5e8
    style UC fill:#fff3e0
    style Models fill:#fff3e0
    style RepoImpl fill:#fce4ec
    style Remote fill:#f3e5f5
    style Local fill:#f3e5f5
```

## Architecture Layers

### 1. **Presentation Layer** (Android-specific)

**Location**: `composeApp/src/androidMain/kotlin/`

**Responsibilities:**
- User interface components using Jetpack Compose
- ViewModels for state management and business logic coordination
- Android-specific services (overlay, recording, accessibility)
- Navigation and user interaction handling

**Key Components:**
- **UI Components**: Compose screens and reusable components
- **ViewModels**: `AudioRecordingViewModel`, `SettingsViewModel`
- **Services**: `AudioRecordingService`, `OverlayService`, `WhisperTopAccessibilityService`
- **Managers**: Permission handling, service coordination

**Dependencies**: Domain layer (Use Cases, Models)

### 2. **Domain Layer** (Shared)

**Location**: `shared/src/commonMain/kotlin/.../domain/`

**Responsibilities:**
- Core business logic and rules
- Use case implementations
- Domain models and entities
- Repository interfaces (contracts)

**Key Components:**
- **Use Cases**: `StartRecordingUseCase`, `StopRecordingUseCase`, `ApiKeyUseCase`
- **Models**: `AppSettings`, `AudioFile`, `TranscriptionRequest/Response`, `RecordingState`
- **Repository Interfaces**: `AudioRepository`, `SettingsRepository`, `TranscriptionRepository`
- **Services**: Abstract service interfaces for platform-specific implementations

**Dependencies**: None (pure business logic)

### 3. **Data Layer** (Shared)

**Location**: `shared/src/commonMain/kotlin/.../data/`

**Responsibilities:**
- Repository implementations
- Data source abstractions
- API communication
- Local data persistence
- Data transformation between layers

**Key Components:**
- **Repositories**: Concrete implementations of domain interfaces
- **Remote Data Sources**: OpenAI API client, HTTP communication
- **Local Data Sources**: Database DAOs, SharedPreferences/DataStore
- **Models**: DTOs for API communication, Entity models for database

**Dependencies**: Platform layer for concrete implementations

### 4. **Platform Layer** (Platform-specific)

**Locations**: 
- `shared/src/androidMain/kotlin/`
- `shared/src/iosMain/kotlin/` (future)

**Responsibilities:**
- Platform-specific implementations using expect/actual pattern
- Hardware access (microphone, storage)
- Platform APIs integration
- Security and encryption

**Key Components:**
- **Audio Recording**: MediaRecorder (Android), AVAudioRecorder (iOS)
- **File System**: Platform-specific file operations
- **Encryption**: Android Keystore, iOS Keychain
- **Database**: Room (Android), Core Data consideration (iOS)

## Data Flow Architecture

```mermaid
sequenceDiagram
    participant U as User
    participant UI as UI Component
    participant VM as ViewModel
    participant UC as Use Case
    participant R as Repository
    participant DS as Data Source
    participant API as OpenAI API
    
    U->>UI: Tap record button
    UI->>VM: startRecording()
    VM->>UC: execute(StartRecordingUseCase)
    UC->>R: startAudioRecording()
    R->>DS: beginRecording()
    DS-->>R: Recording started
    R-->>UC: Success
    UC-->>VM: RecordingState.Recording
    VM-->>UI: Update UI state
    UI-->>U: Show recording indicator
    
    Note over U,API: Recording in progress...
    
    U->>UI: Tap stop button
    UI->>VM: stopRecording()
    VM->>UC: execute(StopRecordingUseCase)
    UC->>R: stopAndTranscribe()
    R->>DS: stopRecording()
    DS-->>R: Audio data
    R->>API: transcribe(audioData)
    API-->>R: Transcription result
    R->>DS: saveTranscription()
    R-->>UC: Success(transcription)
    UC-->>VM: RecordingState.Success
    VM-->>UI: Show transcription
    UI-->>U: Display text result
```

## Dependency Injection Architecture

**Framework**: Koin

```mermaid
graph LR
    subgraph "Application Module"
        AppMod[Application Module<br/>Android-specific dependencies]
    end
    
    subgraph "Shared Module"
        SharedMod[Shared Module<br/>Cross-platform dependencies]
    end
    
    subgraph "Platform Modules"
        AndroidMod[Android Module<br/>Platform implementations]
        iOSMod[iOS Module<br/>Future platform]
    end
    
    AppMod --> SharedMod
    AppMod --> AndroidMod
    SharedMod --> AndroidMod
    SharedMod --> iOSMod
    
    style AppMod fill:#e1f5fe
    style SharedMod fill:#fff3e0
    style AndroidMod fill:#e8f5e8
    style iOSMod fill:#ffebee
```

**Koin Module Structure:**

```kotlin
// Shared module (commonMain)
val sharedModule = module {
    // Use Cases
    single<StartRecordingUseCase> { StartRecordingUseCase(get(), get()) }
    single<StopRecordingUseCase> { StopRecordingUseCase(get(), get()) }
    
    // Repository Interfaces -> Implementations
    single<AudioRepository> { AudioRepositoryImpl(get(), get()) }
    single<SettingsRepository> { SettingsRepositoryImpl(get()) }
    single<TranscriptionRepository> { TranscriptionRepositoryImpl(get(), get()) }
}

// Android module (androidMain)
val androidModule = module {
    // Platform-specific implementations
    single<AudioRecorderService> { AndroidAudioRecorderService(androidContext()) }
    single<FileReaderService> { AndroidFileReaderService(androidContext()) }
    single<PreferencesDataSource> { createPreferencesDataSource(androidContext()) }
}

// Application module (Android app)
val applicationModule = module {
    // ViewModels
    viewModel { AudioRecordingViewModel(get(), get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
    
    // Android Services
    single<OverlayService> { OverlayService() }
    single<AudioRecordingService> { AudioRecordingService() }
}
```

## State Management

### Recording State Machine

```mermaid
stateDiagram-v2
    [*] --> Idle
    
    Idle --> Recording : startRecording()
    Recording --> Processing : stopRecording()
    Recording --> Idle : cancelRecording()
    Processing --> Success : transcription complete
    Processing --> Error : transcription failed
    Success --> Idle : reset()
    Error --> Idle : reset()
    Error --> Recording : retry()
    
    note right of Recording
        - Timer active
        - Audio capture
        - Visual feedback
    end note
    
    note right of Processing
        - Upload audio
        - API call
        - Progress indicator
    end note
```

**State Implementation:**

```kotlin
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(
        val startTime: Long,
        val duration: Long = 0L
    ) : RecordingState()
    
    data class Processing(
        val progress: Float = 0f
    ) : RecordingState()
    
    data class Success(
        val audioFile: AudioFile,
        val transcription: String
    ) : RecordingState()
    
    data class Error(
        val throwable: Throwable,
        val retryable: Boolean = true
    ) : RecordingState()
}
```

## Component Architecture

### Audio Recording Component

```mermaid
graph TD
    subgraph "Audio Recording System"
        ARS[AudioRecordingService<br/>Foreground Service]
        ARM[AudioRecordingManager<br/>Coordination]
        AR[AudioRecorder<br/>Platform Implementation]
        WAV[WAV File Generator<br/>Audio Processing]
    end
    
    subgraph "Overlay System"
        OS[OverlayService<br/>System Overlay]
        MBO[MicButtonOverlay<br/>Floating UI]
        DM[DragManager<br/>Touch Handling]
    end
    
    subgraph "Accessibility System"
        WAS[WhisperTopAccessibilityService<br/>Text Insertion]
        TIU[TextInsertionUtils<br/>Accessibility API]
        PERM[PermissionManager<br/>Permission Handling]
    end
    
    ARS --> ARM
    ARM --> AR
    ARM --> WAV
    OS --> MBO
    MBO --> DM
    WAS --> TIU
    WAS --> PERM
    
    ARM -.-> OS : Start/Stop Overlay
    TIU -.-> ARM : Text Insertion Events
```

### Settings and Configuration

```mermaid
graph TD
    subgraph "Settings Management"
        SM[SettingsManager<br/>Configuration Hub]
        AS[AppSettings<br/>Domain Model]
        PDS[PreferencesDataSource<br/>Encrypted Storage]
        MH[MigrationHelper<br/>Version Management]
    end
    
    subgraph "API Configuration"
        AC[ApiConfiguration<br/>OpenAI Settings]
        KC[KeyManager<br/>API Key Security]
        EP[EndpointValidator<br/>Custom Endpoint Support]
    end
    
    subgraph "Theme and UI"
        TM[ThemeManager<br/>Material 3 Theming]
        TC[ThemeConfiguration<br/>User Preferences]
        DC[DynamicColor<br/>Material You]
    end
    
    SM --> AS
    AS --> PDS
    SM --> MH
    AC --> KC
    AC --> EP
    TM --> TC
    TC --> DC
    
    SM -.-> AC : API Settings
    SM -.-> TM : Theme Settings
```

## Performance Architecture

### Memory Management

```mermaid
graph LR
    subgraph "Memory Optimization"
        OC[Object Caching<br/>Repository Level]
        LP[Lazy Properties<br/>Kotlin Lazy]
        WR[Weak References<br/>Callback Management]
        GC[Garbage Collection<br/>Lifecycle Awareness]
    end
    
    subgraph "Audio Memory"
        AB[Audio Buffers<br/>Circular Buffers]
        SM[Streaming Mode<br/>Large File Handling]
        CM[Compression<br/>WAV Optimization]
        TF[Temp Files<br/>Cleanup Strategy]
    end
    
    OC --> LP
    LP --> WR
    WR --> GC
    AB --> SM
    SM --> CM
    CM --> TF
```

### Database Performance

```mermaid
graph TD
    subgraph "Database Optimization"
        CI[Composite Indexes<br/>Query Optimization]
        QP[Query Planning<br/>Execution Strategy]
        CC[Connection Caching<br/>Pool Management]
        BG[Background Operations<br/>Coroutines]
    end
    
    subgraph "Data Lifecycle"
        RP[Retention Policies<br/>Automatic Cleanup]
        AC[Archival Compression<br/>Storage Optimization]
        EP[Export Pipeline<br/>Data Portability]
        BC[Backup Creation<br/>Recovery Strategy]
    end
    
    CI --> QP
    QP --> CC
    CC --> BG
    RP --> AC
    AC --> EP
    EP --> BC
```

## Security Architecture

```mermaid
graph TB
    subgraph "Encryption Layer"
        AK[Android Keystore<br/>Hardware Security]
        SC[SQLCipher<br/>Database Encryption]
        EP[Encrypted Preferences<br/>Settings Security]
        KM[Key Management<br/>Rotation & Backup]
    end
    
    subgraph "API Security"
        AS[API Key Security<br/>Secure Storage]
        TLS[TLS Communication<br/>Certificate Pinning]
        RV[Request Validation<br/>Input Sanitization]
        RT[Rate Limiting<br/>Abuse Prevention]
    end
    
    subgraph "Privacy Protection"
        DL[Data Lifecycle<br/>Retention Management]
        AL[Audit Logging<br/>Access Tracking]
        UP[User Permissions<br/>Privacy Controls]
        DE[Data Encryption<br/>At-Rest & In-Transit]
    end
    
    AK --> SC
    SC --> EP
    EP --> KM
    AS --> TLS
    TLS --> RV
    RV --> RT
    DL --> AL
    AL --> UP
    UP --> DE
```

## Testing Architecture

### Test Layer Structure

```mermaid
graph TD
    subgraph "Unit Tests"
        DT[Domain Tests<br/>Business Logic]
        RT[Repository Tests<br/>Data Layer]
        VT[ViewModel Tests<br/>Presentation Logic]
        UT[Utility Tests<br/>Helper Functions]
    end
    
    subgraph "Integration Tests"
        AT[API Tests<br/>Network Layer]
        DB[Database Tests<br/>Room Integration]
        ST[Service Tests<br/>Android Services]
        CT[Component Tests<br/>UI Components]
    end
    
    subgraph "UI Tests"
        ET[End-to-End Tests<br/>User Workflows]
        IT[Instrumented Tests<br/>Device Testing]
        PT[Performance Tests<br/>Benchmarking]
        AT_UI[Accessibility Tests<br/>A11y Compliance]
    end
    
    DT --> RT
    RT --> VT
    VT --> UT
    AT --> DB
    DB --> ST
    ST --> CT
    ET --> IT
    IT --> PT
    PT --> AT_UI
```

## Platform-Specific Implementations

### Android Architecture

```mermaid
graph TB
    subgraph "Android Components"
        MA[MainActivity<br/>Entry Point]
        FS[Foreground Services<br/>Background Processing]
        BR[Broadcast Receivers<br/>System Events]
        CP[Content Providers<br/>Data Sharing]
    end
    
    subgraph "Android Services"
        ARS[AudioRecordingService<br/>Recording Management]
        OS[OverlayService<br/>System Overlay]
        AS[AccessibilityService<br/>Text Insertion]
        TS[TileService<br/>Quick Settings]
    end
    
    subgraph "Android Storage"
        R[Room Database<br/>Local Storage]
        DS[DataStore<br/>Preferences]
        IF[Internal Files<br/>Audio Storage]
        AK[Android Keystore<br/>Security]
    end
    
    MA --> FS
    FS --> BR
    BR --> CP
    ARS --> OS
    OS --> AS
    AS --> TS
    R --> DS
    DS --> IF
    IF --> AK
```

### Future iOS Architecture

```mermaid
graph TB
    subgraph "iOS Components (Future)"
        VC[View Controllers<br/>UI Management]
        BG[Background Tasks<br/>iOS Background Processing]
        NS[Notification Service<br/>System Integration]
        EX[App Extensions<br/>Widget Support]
    end
    
    subgraph "iOS Services"
        AVF[AVFoundation<br/>Audio Recording]
        CK[CloudKit<br/>Data Sync]
        KC[Keychain<br/>Security]
        CD[Core Data<br/>Local Storage]
    end
    
    VC --> BG
    BG --> NS
    NS --> EX
    AVF --> CK
    CK --> KC
    KC --> CD
    
    style VC fill:#ffebee
    style BG fill:#ffebee
    style NS fill:#ffebee
    style EX fill:#ffebee
    style AVF fill:#ffebee
    style CK fill:#ffebee
    style KC fill:#ffebee
    style CD fill:#ffebee
```

## Error Handling Architecture

### Error Classification

```mermaid
graph TD
    subgraph "Error Types"
        NE[Network Errors<br/>API, Connectivity]
        PE[Permission Errors<br/>Audio, Overlay, A11y]
        SE[Service Errors<br/>Recording, Processing]
        DE[Data Errors<br/>Database, Storage]
    end
    
    subgraph "Error Recovery"
        RT[Retry Logic<br/>Exponential Backoff]
        FB[Fallback Mechanisms<br/>Offline Mode]
        UF[User Feedback<br/>Error Messages]
        LG[Error Logging<br/>Analytics]
    end
    
    subgraph "Error Reporting"
        CR[Crash Reporting<br/>Stack Traces]
        UM[User Metrics<br/>Error Rates]
        PM[Performance Monitoring<br/>Error Impact]
        AL[Activity Logging<br/>User Actions]
    end
    
    NE --> RT
    PE --> RT
    SE --> FB
    DE --> FB
    RT --> UF
    FB --> UF
    UF --> LG
    LG --> CR
    CR --> UM
    UM --> PM
    PM --> AL
```

## Build and Deployment Architecture

### Build Configuration

```mermaid
graph LR
    subgraph "Build Variants"
        DEBUG[Debug Build<br/>Development]
        RELEASE[Release Build<br/>Production]
        BENCHMARK[Benchmark Build<br/>Performance Testing]
    end
    
    subgraph "Platform Targets"
        AND[Android APK/AAB<br/>Google Play]
        IOS[iOS Framework<br/>Future App Store]
        DESK[Desktop JVM<br/>Future Support]
    end
    
    subgraph "Build Pipeline"
        CI[Continuous Integration<br/>GitHub Actions]
        TEST[Automated Testing<br/>Unit & Integration]
        LINT[Code Quality<br/>Detekt, ktlint]
        SIGN[Code Signing<br/>Release Builds]
    end
    
    DEBUG --> AND
    RELEASE --> AND
    BENCHMARK --> AND
    AND --> IOS
    IOS --> DESK
    CI --> TEST
    TEST --> LINT
    LINT --> SIGN
```

## Conclusion

WhisperTop's architecture is designed for:

- **Scalability**: Clean separation of concerns and modular design
- **Testability**: Dependency injection and interface-based design
- **Cross-platform**: Kotlin Multiplatform with shared business logic
- **Performance**: Optimized data access and memory management
- **Security**: Comprehensive encryption and privacy protection
- **Maintainability**: Clear architectural boundaries and documentation

The architecture supports current Android development while providing a foundation for future iOS expansion and additional platform support.

For detailed implementation examples, see:
- [API Documentation](../api/README.md)
- [Database Schema](../DATABASE_SCHEMA.md)
- [Performance Guidelines](../performance/README.md)
- [Testing Strategy](../TESTING.md)