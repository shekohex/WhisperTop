# Component Interaction Diagrams

## Overview

This document provides detailed component interaction diagrams showing how different parts of WhisperTop work together to deliver core functionality.

## Core Recording Workflow

### Complete Transcription Flow

```mermaid
sequenceDiagram
    participant User
    participant FloatingButton as Floating Mic Button
    participant OverlayService as Overlay Service
    participant AudioService as Audio Recording Service
    participant RecordingManager as Recording Manager
    participant AudioRecorder as Audio Recorder (Platform)
    participant OpenAIAPI as OpenAI API
    participant AccessibilityService as Accessibility Service
    participant Database as Database
    participant SettingsRepo as Settings Repository

    %% Initialize overlay
    User->>OverlayService: App launches
    OverlayService->>FloatingButton: Create floating button
    FloatingButton-->>User: Button appears on screen

    %% Start recording
    User->>FloatingButton: Long press to record
    FloatingButton->>OverlayService: onRecordStart()
    OverlayService->>AudioService: startRecording()
    AudioService->>RecordingManager: beginRecording()
    RecordingManager->>SettingsRepo: getAudioSettings()
    SettingsRepo-->>RecordingManager: AudioConfig
    RecordingManager->>AudioRecorder: startRecording(config)
    AudioRecorder-->>RecordingManager: Recording started
    RecordingManager-->>AudioService: RecordingState.Recording
    AudioService-->>OverlayService: State update
    OverlayService->>FloatingButton: Update UI (recording state)
    FloatingButton-->>User: Visual feedback (red, pulsing)

    %% Recording in progress
    Note over AudioRecorder: Audio capture active
    AudioRecorder->>AudioRecorder: Capture audio buffers
    RecordingManager->>FloatingButton: Duration updates
    FloatingButton-->>User: Timer display

    %% Stop recording
    User->>FloatingButton: Release button
    FloatingButton->>OverlayService: onRecordStop()
    OverlayService->>AudioService: stopRecording()
    AudioService->>RecordingManager: stopAndProcess()
    RecordingManager->>AudioRecorder: stopRecording()
    AudioRecorder-->>RecordingManager: WAV audio data
    RecordingManager-->>AudioService: RecordingState.Processing
    AudioService-->>OverlayService: State update
    OverlayService->>FloatingButton: Update UI (processing)
    FloatingButton-->>User: Processing indicator (blue)

    %% Transcription
    RecordingManager->>SettingsRepo: getApiSettings()
    SettingsRepo-->>RecordingManager: API config (key, model, etc.)
    RecordingManager->>OpenAIAPI: transcribe(audioData, config)
    OpenAIAPI-->>RecordingManager: Transcription response
    RecordingManager->>Database: saveTranscription()
    Database-->>RecordingManager: Saved
    RecordingManager-->>AudioService: RecordingState.Success(text)
    AudioService-->>OverlayService: State update
    OverlayService->>FloatingButton: Update UI (success)
    FloatingButton-->>User: Success feedback (green)

    %% Text insertion
    RecordingManager->>AccessibilityService: insertText(transcription)
    AccessibilityService->>AccessibilityService: findFocusedNode()
    AccessibilityService->>AccessibilityService: performTextInsertion()
    AccessibilityService-->>RecordingManager: Insertion result
    RecordingManager-->>AudioService: Final state update
    AudioService-->>OverlayService: Complete
    OverlayService->>FloatingButton: Reset to idle
    FloatingButton-->>User: Ready for next recording
```

## Settings and Configuration Flow

### Settings Management Interaction

```mermaid
graph TD
    subgraph "User Interface Layer"
        SS[Settings Screen]
        MS[Model Selection]
        TS[Theme Settings]
        AS[API Settings]
    end
    
    subgraph "ViewModel Layer"
        SVM[Settings ViewModel]
        MSVM[Model Selection ViewModel]
        TSVM[Theme Settings ViewModel]
    end
    
    subgraph "Use Case Layer"
        GS[Get Settings Use Case]
        US[Update Settings Use Case]
        VS[Validate Settings Use Case]
        MS_UC[Model Selection Use Case]
    end
    
    subgraph "Repository Layer"
        SR[Settings Repository]
        TR[Theme Repository]
        AR[API Repository]
    end
    
    subgraph "Data Sources"
        PDS[Preferences Data Source<br/>Encrypted]
        AKS[API Key Storage<br/>Android Keystore]
        TCS[Theme Config Storage]
    end
    
    %% UI to ViewModel connections
    SS --> SVM
    MS --> MSVM
    TS --> TSVM
    AS --> SVM
    
    %% ViewModel to Use Case connections
    SVM --> GS
    SVM --> US
    SVM --> VS
    MSVM --> MS_UC
    TSVM --> US
    
    %% Use Case to Repository connections
    GS --> SR
    US --> SR
    VS --> SR
    MS_UC --> AR
    US --> TR
    
    %% Repository to Data Source connections
    SR --> PDS
    AR --> AKS
    TR --> TCS
    
    style SS fill:#e1f5fe
    style SVM fill:#e8f5e8
    style GS fill:#fff3e0
    style SR fill:#fce4ec
    style PDS fill:#f3e5f5
```

### Settings Persistence Flow

```mermaid
sequenceDiagram
    participant UI as Settings UI
    participant VM as Settings ViewModel
    participant UC as Update Settings Use Case
    participant Repo as Settings Repository
    participant Validator as Settings Validator
    participant PDS as Preferences Data Source
    participant Keystore as Android Keystore
    participant Migration as Migration Helper

    UI->>VM: updateApiKey(newKey)
    VM->>UC: execute(UpdateApiKeyRequest)
    UC->>Validator: validateApiKey(newKey)
    Validator-->>UC: Valid
    UC->>Repo: updateApiKey(newKey)
    Repo->>Keystore: storeSecurely(apiKey)
    Keystore-->>Repo: Stored
    Repo->>PDS: updateSettings(settings)
    
    alt Settings version changed
        PDS->>Migration: checkVersion(currentSettings)
        Migration->>Migration: migrateToLatest(settings)
        Migration-->>PDS: Migrated settings
    end
    
    PDS-->>Repo: Success
    Repo-->>UC: Success
    UC-->>VM: Success
    VM-->>UI: Settings updated
    
    %% Notify dependent components
    VM->>VM: notifySettingsChanged()
    VM-->>AudioService: Settings changed
    VM-->>ThemeManager: Settings changed
```

## Audio Processing Pipeline

### Audio Recording and Processing

```mermaid
graph LR
    subgraph "Audio Capture"
        MIC[Microphone<br/>Hardware]
        AR[AudioRecorder<br/>MediaRecorder/AVAudio]
        AB[Audio Buffers<br/>Circular Buffer]
        AC[Audio Converter<br/>Format Processing]
    end
    
    subgraph "Audio Processing"
        WAV[WAV Generator<br/>Header + PCM Data]
        QA[Quality Analyzer<br/>Audio Validation]
        COMP[Compressor<br/>Size Optimization]
        TF[Temp File<br/>Storage Manager]
    end
    
    subgraph "Upload Pipeline"
        UP[Upload Preparer<br/>Multipart Form]
        HC[HTTP Client<br/>Ktor Client]
        API[OpenAI API<br/>Transcription Service]
        RH[Response Handler<br/>Error Management]
    end
    
    subgraph "Post-Processing"
        TP[Text Processor<br/>Cleanup & Format]
        DB[Database Storage<br/>History & Metrics]
        TI[Text Insertion<br/>Accessibility API]
        NOTIF[Notification<br/>Status Updates]
    end
    
    MIC --> AR
    AR --> AB
    AB --> AC
    AC --> WAV
    WAV --> QA
    QA --> COMP
    COMP --> TF
    TF --> UP
    UP --> HC
    HC --> API
    API --> RH
    RH --> TP
    TP --> DB
    TP --> TI
    TI --> NOTIF
```

### Error Handling in Audio Pipeline

```mermaid
flowchart TD
    Start[Audio Recording Start]
    PermCheck{Audio Permission<br/>Granted?}
    RecStart[Start MediaRecorder]
    RecError{Recording<br/>Error?}
    StopRec[Stop Recording]
    DataValid{Audio Data<br/>Valid?}
    APICall[Call OpenAI API]
    APIError{API<br/>Error?}
    ProcessResp[Process Response]
    Success[Success - Insert Text]
    
    RetryLogic[Retry Logic<br/>Exponential Backoff]
    PermRequest[Request Permission]
    ErrorDialog[Show Error Dialog]
    FallbackMode[Fallback to Manual]
    
    Start --> PermCheck
    PermCheck -->|No| PermRequest
    PermRequest --> PermCheck
    PermCheck -->|Yes| RecStart
    RecStart --> RecError
    RecError -->|Yes| ErrorDialog
    RecError -->|No| StopRec
    StopRec --> DataValid
    DataValid -->|No| ErrorDialog
    DataValid -->|Yes| APICall
    APICall --> APIError
    APIError -->|Yes| RetryLogic
    RetryLogic -->|Retry| APICall
    RetryLogic -->|Give Up| ErrorDialog
    APIError -->|No| ProcessResp
    ProcessResp --> Success
    ErrorDialog --> FallbackMode
    
    style Success fill:#c8e6c9
    style ErrorDialog fill:#ffcdd2
    style RetryLogic fill:#fff3e0
    style FallbackMode fill:#e1f5fe
```

## Overlay System Architecture

### Overlay Service and Window Management

```mermaid
graph TB
    subgraph "System Level"
        WM[WindowManager<br/>Android System Service]
        AS[AccessibilityService<br/>System Service]
        AF[AudioFocus<br/>Audio System]
    end
    
    subgraph "Overlay Service"
        OS[OverlayService<br/>Foreground Service]
        OSM[Overlay State Manager<br/>State Coordination]
        WP[Window Parameters<br/>Layout Configuration]
        LC[Lifecycle Controller<br/>Service Management]
    end
    
    subgraph "Overlay Views"
        MBO[MicButtonOverlay<br/>Compose UI]
        DM[Drag Manager<br/>Touch Events]
        AM[Animation Manager<br/>State Transitions]
        TM[Theme Manager<br/>Visual Styling]
    end
    
    subgraph "Interaction Layer"
        GD[Gesture Detector<br/>Touch Recognition]
        HF[Haptic Feedback<br/>Vibration]
        SF[Sound Feedback<br/>Audio Cues]
        VF[Visual Feedback<br/>Color/Animation]
    end
    
    WM --> OS
    AS --> OS
    AF --> OS
    OS --> OSM
    OSM --> WP
    OSM --> LC
    OS --> MBO
    MBO --> DM
    MBO --> AM
    MBO --> TM
    DM --> GD
    GD --> HF
    GD --> SF
    GD --> VF
```

### Overlay Interaction States

```mermaid
stateDiagram-v2
    [*] --> Hidden
    Hidden --> Visible : Service Started
    
    state Visible {
        [*] --> Idle
        Idle --> Dragging : Drag Start
        Idle --> Recording : Long Press
        Dragging --> Idle : Drop
        Dragging --> EdgeSnap : Near Edge
        EdgeSnap --> Idle : Snap Complete
        Recording --> Processing : Release
        Processing --> Success : API Success
        Processing --> Error : API Error
        Success --> Idle : Timeout
        Error --> Idle : User Dismiss / Timeout
    }
    
    Visible --> Hidden : Service Stopped
    Hidden --> [*] : App Destroyed
    
    note right of Recording
        - Visual: Red, pulsing
        - Haptic: Start vibration
        - Audio: Start tone
        - Timer: Duration display
    end note
    
    note right of Processing
        - Visual: Blue, spinning
        - Haptic: Processing pulse
        - Progress: API call status
    end note
    
    note right of Success
        - Visual: Green, checkmark
        - Haptic: Success pattern
        - Duration: 2 seconds
        - Text: Show transcription preview
    end note
```

## Theme and Configuration System

### Theme Management Flow

```mermaid
sequenceDiagram
    participant User
    participant ThemeUI as Theme Settings UI
    participant ThemeVM as Theme ViewModel
    participant ThemeRepo as Theme Repository
    participant ThemeProvider as Theme Provider
    participant SystemTheme as System Theme Observer
    participant OverlayService as Overlay Service
    participant Database as Database

    %% Theme initialization
    SystemTheme->>ThemeProvider: System theme changed
    ThemeProvider->>ThemeRepo: getThemeSettings()
    ThemeRepo-->>ThemeProvider: Current theme config
    ThemeProvider->>ThemeProvider: calculateEffectiveTheme()
    ThemeProvider-->>OverlayService: Theme updated
    OverlayService->>OverlayService: Update overlay colors

    %% User changes theme
    User->>ThemeUI: Select Dark theme
    ThemeUI->>ThemeVM: updateTheme(DARK)
    ThemeVM->>ThemeRepo: saveTheme(DARK)
    ThemeRepo->>Database: persistThemeChoice()
    Database-->>ThemeRepo: Saved
    ThemeRepo-->>ThemeVM: Success
    ThemeVM->>ThemeProvider: notifyThemeChanged()
    ThemeProvider->>ThemeProvider: recalculateTheme()
    
    %% Broadcast theme change
    ThemeProvider-->>ThemeUI: Theme applied
    ThemeProvider-->>OverlayService: Update overlay theme
    ThemeProvider-->>AudioService: Update notification theme
    
    ThemeUI-->>User: UI theme updated
    OverlayService-->>User: Overlay theme updated
```

### Dynamic Color Integration

```mermaid
graph TD
    subgraph "Color Sources"
        SW[System Wallpaper<br/>Android 12+]
        UC[User Choice<br/>Manual Selection]
        DF[Default Fallback<br/>Material 3 Colors]
    end
    
    subgraph "Color Processing"
        DE[Dynamic Extractor<br/>Material You API]
        CP[Color Processor<br/>Accessibility Compliance]
        CV[Color Validator<br/>Contrast Checking]
        CG[Color Generator<br/>Missing Shades]
    end
    
    subgraph "Theme Application"
        TS[Theme State<br/>Global State]
        LT[Light Theme<br/>Color Scheme]
        DT[Dark Theme<br/>Color Scheme]
        OT[Overlay Theme<br/>System UI Colors]
    end
    
    SW --> DE
    UC --> CP
    DF --> CG
    DE --> CV
    CP --> CV
    CV --> TS
    CG --> TS
    TS --> LT
    TS --> DT
    TS --> OT
    
    style SW fill:#e1f5fe
    style DE fill:#e8f5e8
    style TS fill:#fff3e0
    style OT fill:#fce4ec
```

## Data Synchronization and Migration

### Data Migration Pipeline

```mermaid
sequenceDiagram
    participant App as App Startup
    participant VM as Version Manager
    participant MH as Migration Helper
    participant DB as Database
    participant Prefs as Preferences
    participant Backup as Backup Manager
    participant Validator as Data Validator

    App->>VM: checkAppVersion()
    VM->>DB: getCurrentSchemaVersion()
    DB-->>VM: Current: v3, Target: v4
    VM->>MH: needsMigration(v3, v4)
    MH-->>VM: true
    
    VM->>Backup: createPreMigrationBackup()
    Backup->>DB: exportCurrentData()
    Backup->>Prefs: exportSettings()
    Backup-->>VM: Backup created
    
    VM->>MH: performMigration(v3, v4)
    MH->>DB: runMigrationScript(MIGRATION_3_4)
    DB-->>MH: Schema updated
    MH->>Prefs: migrateSettings()
    Prefs-->>MH: Settings migrated
    MH-->>VM: Migration complete
    
    VM->>Validator: validateMigratedData()
    Validator->>DB: checkDataIntegrity()
    Validator->>Prefs: validateSettings()
    Validator-->>VM: Validation successful
    
    VM->>VM: updateAppVersion()
    VM-->>App: Migration complete
    
    alt Migration fails
        VM->>Backup: restoreFromBackup()
        Backup-->>VM: Restored to v3
        VM-->>App: Migration failed, restored
    end
```

### Settings Migration Detail

```mermaid
flowchart TD
    Start[App Launch]
    VersionCheck{Version Check<br/>Current vs Stored}
    
    V1[Version 1<br/>Basic Settings]
    V2[Version 2<br/>+ Statistics]
    V3[Version 3<br/>+ Theme System]
    V4[Version 4<br/>+ Privacy Controls]
    
    M1_2[Migration 1→2<br/>Add Statistics Defaults]
    M2_3[Migration 2→3<br/>Add Theme Settings]
    M3_4[Migration 3→4<br/>Add Privacy Settings]
    
    Validate[Validate Settings<br/>Check Consistency]
    Success[Migration Complete]
    Error[Migration Error<br/>Restore Backup]
    
    Start --> VersionCheck
    VersionCheck -->|v1 → v4| V1
    VersionCheck -->|v2 → v4| V2  
    VersionCheck -->|v3 → v4| V3
    VersionCheck -->|v4| V4
    
    V1 --> M1_2
    M1_2 --> M2_3
    V2 --> M2_3
    M2_3 --> M3_4
    V3 --> M3_4
    M3_4 --> Validate
    V4 --> Success
    
    Validate -->|Valid| Success
    Validate -->|Invalid| Error
    Error --> Start
    
    style Success fill:#c8e6c9
    style Error fill:#ffcdd2
    style Validate fill:#fff3e0
```

## Performance Monitoring Integration

### Performance Metrics Collection

```mermaid
graph TD
    subgraph "Metric Sources"
        AR[Audio Recording<br/>Duration, Quality]
        API[API Calls<br/>Response Time, Size]
        DB[Database<br/>Query Time, Size]
        UI[UI Rendering<br/>Frame Rate, Jank]
        MEM[Memory Usage<br/>Heap, Native]
    end
    
    subgraph "Collection Layer"
        PM[Performance Monitor<br/>Metrics Aggregation]
        MC[Metrics Collector<br/>Data Processing]
        BW[Background Worker<br/>Async Collection]
        TS[Timestamp Manager<br/>Event Correlation]
    end
    
    subgraph "Storage and Analysis"
        MS[Metrics Storage<br/>Local Database]
        MA[Metrics Analyzer<br/>Pattern Detection]
        AR_ANALYSIS[Alert Rules<br/>Threshold Monitoring]
        RP[Report Generator<br/>Performance Reports]
    end
    
    subgraph "User Interface"
        PD[Performance Dashboard<br/>Real-time Metrics]
        CH[Charts & Graphs<br/>Historical Data]
        AL[Alerts & Warnings<br/>Performance Issues]
        EX[Export Tools<br/>Data Sharing]
    end
    
    AR --> PM
    API --> PM
    DB --> PM
    UI --> PM
    MEM --> PM
    
    PM --> MC
    MC --> BW
    BW --> TS
    
    TS --> MS
    MS --> MA
    MA --> AR_ANALYSIS
    AR_ANALYSIS --> RP
    
    RP --> PD
    PD --> CH
    CH --> AL
    AL --> EX
```

## Testing and Quality Assurance Flow

### Test Execution Pipeline

```mermaid
graph LR
    subgraph "Development"
        CODE[Code Changes<br/>Developer Commits]
        LINT[Static Analysis<br/>detekt, ktlint]
        UNIT[Unit Tests<br/>Business Logic]
        INT[Integration Tests<br/>Component Testing]
    end
    
    subgraph "CI Pipeline"
        BUILD[Build Validation<br/>Gradle Build]
        TEST_RUN[Test Execution<br/>All Test Suites]
        COVERAGE[Coverage Report<br/>Jacoco/Kover]
        QUALITY[Quality Gates<br/>SonarQube]
    end
    
    subgraph "Device Testing"
        EMU[Emulator Tests<br/>Multiple API Levels]
        DEVICE[Physical Device<br/>Real Hardware]
        E2E[End-to-End Tests<br/>User Workflows]
        PERF[Performance Tests<br/>Benchmarking]
    end
    
    subgraph "Release"
        SIGN[Code Signing<br/>Release Keys]
        DEPLOY[Deployment<br/>Play Store]
        MONITOR[Monitoring<br/>Crash Reports]
        FEEDBACK[User Feedback<br/>Analytics]
    end
    
    CODE --> LINT
    LINT --> UNIT
    UNIT --> INT
    INT --> BUILD
    BUILD --> TEST_RUN
    TEST_RUN --> COVERAGE
    COVERAGE --> QUALITY
    QUALITY --> EMU
    EMU --> DEVICE
    DEVICE --> E2E
    E2E --> PERF
    PERF --> SIGN
    SIGN --> DEPLOY
    DEPLOY --> MONITOR
    MONITOR --> FEEDBACK
```

## Conclusion

These component interaction diagrams illustrate how WhisperTop's architecture enables:

1. **Seamless User Experience**: Smooth flow from recording to text insertion
2. **Robust Error Handling**: Comprehensive error recovery at each layer
3. **Performance Optimization**: Efficient data processing and resource management
4. **Flexible Configuration**: Dynamic settings and theme management
5. **Quality Assurance**: Comprehensive testing and monitoring systems

The architecture ensures that each component has clear responsibilities while maintaining loose coupling through well-defined interfaces and dependency injection.