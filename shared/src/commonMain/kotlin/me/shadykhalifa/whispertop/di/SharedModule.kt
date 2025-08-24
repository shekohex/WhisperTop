package me.shadykhalifa.whispertop.di

import io.ktor.client.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import me.shadykhalifa.whispertop.data.remote.createHttpClient
import me.shadykhalifa.whispertop.data.repositories.AudioRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.SettingsRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.TranscriptionRepositoryImpl
// TranscriptionHistoryRepositoryImpl and UserStatisticsRepositoryImpl are platform-specific (androidMain)
// and will be registered in platform modules
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
import me.shadykhalifa.whispertop.domain.models.ErrorMapper
import me.shadykhalifa.whispertop.domain.models.ErrorMapperImpl
import me.shadykhalifa.whispertop.presentation.utils.ViewModelErrorHandler
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
import me.shadykhalifa.whispertop.domain.usecases.DurationTrackerUseCase
import me.shadykhalifa.whispertop.presentation.viewmodels.RecordingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.SettingsViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.ModelSelectionViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.DashboardViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingWpmViewModel
import me.shadykhalifa.whispertop.data.local.ModelSelectionPreferencesManager
// Performance optimization services
import me.shadykhalifa.whispertop.domain.services.PerformanceCacheManager
import me.shadykhalifa.whispertop.domain.services.PerformanceCacheManagerImpl
import me.shadykhalifa.whispertop.domain.services.StatisticsCacheService
import me.shadykhalifa.whispertop.domain.services.StatisticsCacheServiceImpl
import me.shadykhalifa.whispertop.domain.services.BackgroundThreadManager
import me.shadykhalifa.whispertop.domain.services.BackgroundThreadManagerImpl
import me.shadykhalifa.whispertop.domain.services.MemoryProfiler
// import me.shadykhalifa.whispertop.domain.services.CommonMemoryProfiler
import me.shadykhalifa.whispertop.domain.services.PerformanceMonitor
import me.shadykhalifa.whispertop.domain.services.PerformanceMonitorImpl
import me.shadykhalifa.whispertop.domain.services.LazyHistoryLoader
import me.shadykhalifa.whispertop.domain.services.LazyHistoryLoaderImpl
import me.shadykhalifa.whispertop.domain.services.CacheEvictionPolicyManager
import me.shadykhalifa.whispertop.domain.services.CacheEvictionPolicyManagerImpl
import me.shadykhalifa.whispertop.domain.repositories.StatisticsRepository
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
    single<ErrorLoggingService> { ErrorLoggingServiceImpl() }
    singleOf(::ErrorNotificationServiceImpl) { bind<ErrorNotificationService>() }
    single<ErrorMapper> { ErrorMapperImpl(get()) }
    singleOf(::ViewModelErrorHandler)
    singleOf(::ConnectionStatusServiceImpl) { bind<ConnectionStatusService>() }
    
    // Database
    // TODO: Database setup needs to be implemented in androidMain
    // single<AppDatabase> { getDatabase() }
    // single { get<AppDatabase>().transcriptionHistoryDao() }
    // single { get<AppDatabase>().userStatisticsDao() }
    
    // Repositories - Now depend on service interfaces instead of platform-specific implementations
    single<SettingsRepository> { SettingsRepositoryImpl(get(), get<SecurePreferencesRepository>()) }
    singleOf(::AudioRepositoryImpl) { bind<AudioRepository>() }
    singleOf(::TranscriptionRepositoryImpl) { bind<TranscriptionRepository>() }
    // Note: Database-dependent repositories need to be registered in platform modules
    // singleOf(::TranscriptionHistoryRepositoryImpl) { bind<TranscriptionHistoryRepository>() }
    // singleOf(::UserStatisticsRepositoryImpl) { bind<UserStatisticsRepository>() }
    
    // CoroutineScope for managers
    single<CoroutineScope> { CoroutineScope(SupervisorJob() + Dispatchers.Default) }
    
    // Performance Optimization Services
    single<BackgroundThreadManager> { BackgroundThreadManagerImpl() }
    single<PerformanceCacheManager> { PerformanceCacheManagerImpl() }
    // MemoryProfiler is platform-specific and provided in platform modules
    single<PerformanceMonitor> { PerformanceMonitorImpl() }
    // StatisticsCacheService temporarily disabled due to missing StatisticsRepository
    // single<StatisticsCacheService> { StatisticsCacheServiceImpl(get<StatisticsRepository>()) }
    // LazyHistoryLoader temporarily disabled due to missing TranscriptionHistoryRepository
    // single<LazyHistoryLoader> { 
    //     LazyHistoryLoaderImpl(
    //         historyRepository = get<TranscriptionHistoryRepository>(),
    //         performanceMonitor = get(),
    //         backgroundThreadManager = get()
    //     )
    // }
    // CacheEvictionPolicyManager temporarily disabled due to missing dependencies
    // single<CacheEvictionPolicyManager> {
    //     CacheEvictionPolicyManagerImpl(
    //         performanceCacheManager = get(),
    //         statisticsCacheService = get(),
    //         memoryProfiler = get(),
    //         backgroundThreadManager = get(),
    //         coroutineScope = get()
    //     )
    // }
    
    // Managers  
    single { RecordingManager(get()) }
    
    // Use Cases
    factory { StartRecordingUseCase(get(), get(), get<ErrorNotificationService>()) }
    factory { StopRecordingUseCase(get(), get(), get(), get<ErrorNotificationService>()) }
    factory { TranscriptionUseCase(get(), get(), get(), get<ErrorNotificationService>()) }
    factoryOf(::LanguageDetectionUseCase)
    factoryOf(::TranscribeWithLanguageDetectionUseCase)
    single<ServiceManagementUseCase> { ServiceManagementUseCaseImpl(get(), get()) }
    factoryOf(::ServiceInitializationUseCase)
    factoryOf(::PermissionManagementUseCase)
    factoryOf(::ServiceBindingUseCase)
    factoryOf(::UserFeedbackUseCase)
    factoryOf(::DurationTrackerUseCase)
    singleOf(::TranscriptionWorkflowUseCase)
    single<StatisticsCalculatorUseCase> { StatisticsCalculatorUseCaseImpl(get(), get(), get()) }
    
    // ViewModels
    factory { RecordingViewModel(get(), get(), get(), get<ViewModelErrorHandler>()) }
    factory { SettingsViewModel(get<SettingsRepository>(), get<SecurePreferencesRepository>(), get<ViewModelErrorHandler>()) }
    factory { ModelSelectionViewModel(get<ModelSelectionPreferencesManager>(), get<ViewModelErrorHandler>()) }
    // DashboardViewModel temporarily disabled due to missing dependencies
    // factory { DashboardViewModel(get<UserStatisticsRepository>(), get<TranscriptionHistoryRepository>(), get<SettingsRepository>(), get<MetricsCollector>(), get<ViewModelErrorHandler>()) }
    factory { OnboardingWpmViewModel(get<SettingsRepository>(), get<ViewModelErrorHandler>()) }
}