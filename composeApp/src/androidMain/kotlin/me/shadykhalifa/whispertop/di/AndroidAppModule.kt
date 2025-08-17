package me.shadykhalifa.whispertop.di

import me.shadykhalifa.whispertop.data.services.TextInsertionServiceImpl
import me.shadykhalifa.whispertop.data.services.ToastServiceImpl
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
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import me.shadykhalifa.whispertop.presentation.viewmodels.OnboardingViewModel
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
    
    // Platform-specific services
    singleOf(::TextInsertionServiceImpl) { bind<TextInsertionService>() }
    single<ToastService> { ToastServiceImpl(get()) }
    
    // ViewModels
    viewModel { AudioRecordingViewModel() }
    viewModel { OnboardingViewModel() }
}