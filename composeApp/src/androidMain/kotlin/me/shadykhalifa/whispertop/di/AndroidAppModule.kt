package me.shadykhalifa.whispertop.di

import me.shadykhalifa.whispertop.managers.AudioServiceManager
import me.shadykhalifa.whispertop.managers.PermissionHandler
import me.shadykhalifa.whispertop.managers.ServiceRecoveryManager
import me.shadykhalifa.whispertop.presentation.AudioRecordingViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val androidAppModule = module {
    // Service Management
    singleOf(::AudioServiceManager)
    singleOf(::PermissionHandler)
    singleOf(::ServiceRecoveryManager)
    
    // ViewModels
    viewModel { AudioRecordingViewModel() }
}