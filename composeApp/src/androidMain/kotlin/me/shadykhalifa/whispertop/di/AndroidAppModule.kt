package me.shadykhalifa.whispertop.di

import me.shadykhalifa.whispertop.data.permissions.PermissionMonitor
import me.shadykhalifa.whispertop.data.repositories.AndroidPermissionRepository
import me.shadykhalifa.whispertop.data.repositories.AndroidServiceStateRepository
import me.shadykhalifa.whispertop.data.repositories.AndroidUserFeedbackRepository
import me.shadykhalifa.whispertop.data.services.TextInsertionServiceImpl
import me.shadykhalifa.whispertop.data.services.ToastServiceImpl
import me.shadykhalifa.whispertop.domain.repositories.PermissionRepository
import me.shadykhalifa.whispertop.domain.repositories.ServiceStateRepository
import me.shadykhalifa.whispertop.domain.repositories.UserFeedbackRepository
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.domain.services.ToastService
import me.shadykhalifa.whispertop.managers.AndroidSystemSettingsProvider
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.BatteryOptimizationUtil
import me.shadykhalifa.whispertop.managers.OnboardingPermissionManager
import me.shadykhalifa.whispertop.managers.OverlayInitializationManager
import me.shadykhalifa.whispertop.managers.OverlayManager
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.managers.PowerManagementUtil
import me.shadykhalifa.whispertop.managers.PowerAwareApiManager
import me.shadykhalifa.whispertop.managers.ServiceRecoveryManager
import me.shadykhalifa.whispertop.managers.SystemSettingsProvider
import me.shadykhalifa.whispertop.managers.createApiManager
import me.shadykhalifa.whispertop.domain.usecases.PermissionManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.ServiceManagementUseCase
import me.shadykhalifa.whispertop.domain.usecases.TranscriptionWorkflowUseCase
import me.shadykhalifa.whispertop.domain.usecases.UserFeedbackUseCase
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.PermissionsViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers

val androidAppModule = module {
    // Service Management
    singleOf(::AudioServiceManager)
    singleOf(::PermissionHandler)
    singleOf(::OnboardingPermissionManager)
    singleOf(::OverlayManager)
    singleOf(::OverlayInitializationManager)
    
    // System Settings Abstraction
    single<SystemSettingsProvider> { AndroidSystemSettingsProvider(get()) }
    single { ServiceRecoveryManager(get()) }
    single { BatteryOptimizationUtil(get()) }
    single { PowerManagementUtil(get()) }
    single { 
        val powerManager = get<PowerManagementUtil>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        powerManager.createApiManager(scope)
    }
    
    // Platform-specific repositories
    singleOf(::AndroidServiceStateRepository) { bind<ServiceStateRepository>() }
    singleOf(::AndroidPermissionRepository) { bind<PermissionRepository>() }
    singleOf(::AndroidUserFeedbackRepository) { bind<UserFeedbackRepository>() }
    
    // Permission management
    single { PermissionMonitor(get()) }
    
    // Platform-specific services
    singleOf(::TextInsertionServiceImpl) { bind<TextInsertionService>() }
    single<ToastService> { ToastServiceImpl(get()) }
    
    // ViewModels
    viewModel { 
        AudioRecordingViewModel(
            serviceManagementUseCase = get(),
            permissionManagementUseCase = get(),
            transcriptionWorkflowUseCase = get(),
            userFeedbackUseCase = get()
        )
    }
    viewModel { OnboardingViewModel() }
    viewModel { 
        PermissionsViewModel(
            context = get(),
            permissionRepository = get(),
            permissionMonitor = get()
        )
    }
}