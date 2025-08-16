package me.shadykhalifa.whispertop.di

import me.shadykhalifa.whispertop.data.services.TextInsertionServiceImpl
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.managers.ServiceRecoveryManager
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidAppModule = module {
    // Service Management
    singleOf(::AudioServiceManager)
    singleOf(::PermissionHandler)
    singleOf(::ServiceRecoveryManager)
    
    // Platform-specific services
    singleOf(::TextInsertionServiceImpl) { bind<TextInsertionService>() }
    
    // ViewModels
    viewModel { AudioRecordingViewModel() }
}