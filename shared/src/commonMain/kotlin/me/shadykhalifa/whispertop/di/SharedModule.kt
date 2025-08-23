package me.shadykhalifa.whispertop.di

import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.shadykhalifa.whispertop.data.remote.createHttpClient
import me.shadykhalifa.whispertop.data.repositories.AudioRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.SettingsRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.TranscriptionRepositoryImpl
// TODO: These repository implementations need to be created
// import me.shadykhalifa.whispertop.data.repositories.TranscriptionHistoryRepositoryImpl
// import me.shadykhalifa.whispertop.data.repositories.UserStatisticsRepositoryImpl
import me.shadykhalifa.whispertop.data.services.AudioCacheServiceImpl
import me.shadykhalifa.whispertop.data.services.AudioRecorderServiceImpl
import me.shadykhalifa.whispertop.data.services.FileReaderServiceImpl
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.domain.repositories.UserFeedbackRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.domain.services.AudioCacheService
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.domain.services.RetryService
import me.shadykhalifa.whispertop.domain.services.RetryServiceImpl
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingServiceImpl
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusService
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusServiceImpl
import me.shadykhalifa.whispertop.domain.services.LoggingManager
import me.shadykhalifa.whispertop.data.services.LoggingManagerImpl
import me.shadykhalifa.whispertop.data.services.LoggingFactory
import me.shadykhalifa.whispertop.domain.services.MetricsCollector
import me.shadykhalifa.whispertop.data.services.MetricsCollectorImpl
import me.shadykhalifa.whispertop.domain.services.DebugLoggingService
import me.shadykhalifa.whispertop.data.services.DebugLoggingServiceImpl
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationServiceImpl
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.usecases.StartRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.StopRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.LanguageDetectionUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscribeWithLanguageDetectionUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceManagementUseCaseImpl
import me.shadykhalifa.whispertop.domain.usecases.ServiceInitializationUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceBindingUseCase
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.domain.usecases.StatisticsCalculatorUseCase
import me.shadykhalifa.whispertop.domain.usecases.StatisticsCalculatorUseCaseImpl
import me.shadykhalifa.whispertop.presentation.viewmodels.RecordingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionViewModel
import me.shadykhalifa.whispertop.data.local.ModelSelectionPreferencesManager
// TODO: AppDatabase needs to be created or moved to androidMain
// import me.shadykhalifa.whispertop.data.database.AppDatabase
// import me.shadykhalifa.whispertop.data.database.getDatabase
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val sharedModule = module {
    // HTTP Client with debug capabilities
    single<HttpClient> { 
        createHttpClient()
    }
    
    // Service Adapters - Bridge between domain interfaces and platform implementations
    single<AudioRecorderService> { AudioRecorderServiceImpl(get()) }
    single<FileReaderService> { FileReaderServiceImpl(get()) }
    singleOf(::AudioCacheServiceImpl) { bind<AudioCacheService>() }
    
    // Model Selection Preferences Manager
    singleOf(::ModelSelectionPreferencesManager)
    
    // Logging Services
    single<LoggingManager> { LoggingFactory.createDefaultLoggingManager() }
    
    // Performance Monitoring Services
    single<MetricsCollector> { MetricsCollectorImpl(get()) }
    
    // Debug Logging Services
    single<DebugLoggingService> { DebugLoggingServiceImpl(get()) }
    
    // Error Handling Services
    singleOf(::RetryServiceImpl) { bind<RetryService>() }
    singleOf(::ErrorLoggingServiceImpl) { bind<ErrorLoggingService>() }
    singleOf(::ErrorNotificationServiceImpl) { bind<ErrorNotificationService>() }
    singleOf(::ConnectionStatusServiceImpl) { bind<ConnectionStatusService>() }
    
    // Database
    // TODO: Database setup needs to be implemented in androidMain
    // single<AppDatabase> { getDatabase() }
    // single { get<AppDatabase>().transcriptionHistoryDao() }
    // single { get<AppDatabase>().userStatisticsDao() }
    
    // Repositories - Now depend on service interfaces instead of platform-specific implementations
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    singleOf(::AudioRepositoryImpl) { bind<AudioRepository>() }
    singleOf(::TranscriptionRepositoryImpl) { bind<TranscriptionRepository>() }
    // Database repositories will be registered in Android module after database components
    
    // CoroutineScope for managers
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Managers  
    single { RecordingManager(get()) }
    
    // Use Cases
    factoryOf(::StartRecordingUseCase)
    factoryOf(::StopRecordingUseCase)
    factoryOf(::TranscriptionUseCase)
    factoryOf(::LanguageDetectionUseCase)
    factoryOf(::TranscribeWithLanguageDetectionUseCase)
    single<ServiceManagementUseCase> { ServiceManagementUseCaseImpl(get(), get()) }
    factoryOf(::ServiceInitializationUseCase)
    factoryOf(::PermissionManagementUseCase)
    factoryOf(::ServiceBindingUseCase)
    factoryOf(::UserFeedbackUseCase)
    singleOf(::TranscriptionWorkflowUseCase)
    single<StatisticsCalculatorUseCase> { StatisticsCalculatorUseCaseImpl(get(), get(), get()) }
    
    // ViewModels
    singleOf(::RecordingViewModel)
    factory { SettingsViewModel(get<SettingsRepository>(), get<SecurePreferencesRepository>()) }
    singleOf(::ModelSelectionViewModel)
}