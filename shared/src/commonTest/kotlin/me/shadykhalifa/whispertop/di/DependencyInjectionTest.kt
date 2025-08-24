package me.shadykhalifa.whispertop.di

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.usecases.StartRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.StopRecordingUseCase
import me.shadykhalifa.whispertop.presentation.viewmodels.RecordingViewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.get
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DependencyInjectionTest : KoinTest {
    
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        startKoin {
            modules(listOf(sharedModule, mockPlatformModule))
        }
    }
    
    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }
    
    @Test
    fun `should inject HttpClient from shared module`() {
        val httpClient: HttpClient = get()
        assertNotNull(httpClient, "HttpClient should be injected")
    }
    
    @Test
    fun `should inject platform-specific dependencies from mock module`() {
        val preferencesDataSource: PreferencesDataSource = get()
        val audioRecorderService: AudioRecorderService = get()
        val fileReaderService: FileReaderService = get()
        
        assertNotNull(preferencesDataSource, "PreferencesDataSource should be injected")
        assertNotNull(audioRecorderService, "AudioRecorderService should be injected")
        assertNotNull(fileReaderService, "FileReaderService should be injected")
        
        // Verify they are the mock implementations
        assertTrue(preferencesDataSource is MockPreferencesDataSource, "Should use mock PreferencesDataSource")
        assertTrue(audioRecorderService is MockAudioRecorderService, "Should use mock AudioRecorderService")
        assertTrue(fileReaderService is MockFileReaderService, "Should use mock FileReaderService")
    }
    
    @Test
    fun `should inject repository implementations with proper dependencies`() {
        val audioRepository: AudioRepository = get()
        val settingsRepository: SettingsRepository = get()
        val transcriptionRepository: TranscriptionRepository = get()
        
        assertNotNull(audioRepository, "AudioRepository should be injected")
        assertNotNull(settingsRepository, "SettingsRepository should be injected")
        assertNotNull(transcriptionRepository, "TranscriptionRepository should be injected")
    }
    
    @Test
    fun `should inject use cases with repository dependencies`() {
        val startRecordingUseCase: StartRecordingUseCase = get()
        val stopRecordingUseCase: StopRecordingUseCase = get()
        
        assertNotNull(startRecordingUseCase, "StartRecordingUseCase should be injected")
        assertNotNull(stopRecordingUseCase, "StopRecordingUseCase should be injected")
    }
    
    @Test
    fun `should inject view models with all dependencies`() {
        val recordingViewModel: RecordingViewModel = get()
        assertNotNull(recordingViewModel, "RecordingViewModel should be injected")
    }
    
    @Test
    fun `should provide same singleton instances for repositories`() {
        val audioRepository1: AudioRepository = get()
        val audioRepository2: AudioRepository = get()
        val settingsRepository1: SettingsRepository = get()
        val settingsRepository2: SettingsRepository = get()
        
        assertTrue(audioRepository1 === audioRepository2, "AudioRepository should be singleton")
        assertTrue(settingsRepository1 === settingsRepository2, "SettingsRepository should be singleton")
    }
    
    @Test
    fun `should provide different factory instances for use cases`() {
        val startRecordingUseCase1: StartRecordingUseCase = get()
        val startRecordingUseCase2: StartRecordingUseCase = get()
        val stopRecordingUseCase1: StopRecordingUseCase = get()
        val stopRecordingUseCase2: StopRecordingUseCase = get()
        
        assertTrue(startRecordingUseCase1 !== startRecordingUseCase2, "StartRecordingUseCase should be factory")
        assertTrue(stopRecordingUseCase1 !== stopRecordingUseCase2, "StopRecordingUseCase should be factory")
    }
    
    @Test
    fun `should have complete SOLID-compliant dependency graph without circular dependencies`() {
        // This test verifies that our SOLID-compliant design works correctly
        // and there are no circular dependencies
        
        try {
            // Infrastructure
            val httpClient: HttpClient = get()
            val preferencesDataSource: PreferencesDataSource = get()
            
            // Service interfaces (following Dependency Inversion Principle)
            val audioRecorderService: AudioRecorderService = get()
            val fileReaderService: FileReaderService = get()
            
            // Repository layer (depends on service interfaces, not concrete classes)
            val audioRepository: AudioRepository = get()
            val settingsRepository: SettingsRepository = get()
            val transcriptionRepository: TranscriptionRepository = get()
            
            // Use case layer (depends on repository abstractions)
            val startRecordingUseCase: StartRecordingUseCase = get()
            val stopRecordingUseCase: StopRecordingUseCase = get()
            
            // Presentation layer
            val recordingViewModel: RecordingViewModel = get()
            
            // If we get here without exceptions, the dependency graph is valid
            assertNotNull(httpClient, "HttpClient should be resolved")
            assertNotNull(preferencesDataSource, "PreferencesDataSource should be resolved")
            assertNotNull(audioRecorderService, "AudioRecorderService should be resolved")
            assertNotNull(fileReaderService, "FileReaderService should be resolved")
            assertNotNull(audioRepository, "AudioRepository should be resolved")
            assertNotNull(settingsRepository, "SettingsRepository should be resolved")
            assertNotNull(transcriptionRepository, "TranscriptionRepository should be resolved")
            assertNotNull(startRecordingUseCase, "StartRecordingUseCase should be resolved")
            assertNotNull(stopRecordingUseCase, "StopRecordingUseCase should be resolved")
            assertNotNull(recordingViewModel, "RecordingViewModel should be resolved")
            
            // Verify mock implementations are used (proving we can swap implementations)
            assertTrue(preferencesDataSource is MockPreferencesDataSource, "Should use mock PreferencesDataSource")
            assertTrue(audioRecorderService is MockAudioRecorderService, "Should use mock AudioRecorderService")
            assertTrue(fileReaderService is MockFileReaderService, "Should use mock FileReaderService")
            
        } catch (e: Exception) {
            throw AssertionError("SOLID-compliant dependency injection failed: ${e.message}", e)
        }
    }
}