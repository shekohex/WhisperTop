package me.shadykhalifa.whispertop.di

import io.ktor.client.*
import me.shadykhalifa.whispertop.data.remote.createHttpClient
import me.shadykhalifa.whispertop.data.repositories.AudioRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.SettingsRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.TranscriptionRepositoryImpl
import me.shadykhalifa.whispertop.data.services.AudioCacheServiceImpl
import me.shadykhalifa.whispertop.data.services.AudioRecorderServiceImpl
import me.shadykhalifa.whispertop.data.services.FileReaderServiceImpl
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.services.AudioCacheService
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.domain.services.RetryService
import me.shadykhalifa.whispertop.domain.services.RetryServiceImpl
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingServiceImpl
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusService
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusServiceImpl
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationServiceImpl
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.usecases.StartRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.StopRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.presentation.viewmodels.RecordingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionViewModel
import me.shadykhalifa.whispertop.data.local.ModelSelectionPreferencesManager
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    // HTTP Client
    single<HttpClient> { createHttpClient() }
    
    // Service Adapters - Bridge between domain interfaces and platform implementations
    single<AudioRecorderService> { AudioRecorderServiceImpl(get()) }
    single<FileReaderService> { FileReaderServiceImpl(get()) }
    singleOf(::AudioCacheServiceImpl) { bind<AudioCacheService>() }
    
    // Model Selection Preferences Manager
    singleOf(::ModelSelectionPreferencesManager)
    
    // Error Handling Services
    singleOf(::RetryServiceImpl) { bind<RetryService>() }
    singleOf(::ErrorLoggingServiceImpl) { bind<ErrorLoggingService>() }
    singleOf(::ErrorNotificationServiceImpl) { bind<ErrorNotificationService>() }
    singleOf(::ConnectionStatusServiceImpl) { bind<ConnectionStatusService>() }
    
    // Repositories - Now depend on service interfaces instead of platform-specific implementations
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    singleOf(::AudioRepositoryImpl) { bind<AudioRepository>() }
    singleOf(::TranscriptionRepositoryImpl) { bind<TranscriptionRepository>() }
    
    // Managers
    singleOf(::RecordingManager)
    
    // Use Cases
    factoryOf(::StartRecordingUseCase)
    factoryOf(::StopRecordingUseCase)
    factoryOf(::TranscriptionUseCase)
    singleOf(::TranscriptionWorkflowUseCase)
    
    // ViewModels
    singleOf(::RecordingViewModel)
    factory { SettingsViewModel(get<SettingsRepository>(), get<SecurePreferencesRepository>()) }
    singleOf(::ModelSelectionViewModel)
}