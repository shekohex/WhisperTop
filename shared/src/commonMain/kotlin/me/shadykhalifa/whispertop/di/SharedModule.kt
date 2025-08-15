package me.shadykhalifa.whispertop.di

import io.ktor.client.*
import me.shadykhalifa.whispertop.data.remote.createHttpClient
import me.shadykhalifa.whispertop.data.repositories.AudioRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.SettingsRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.TranscriptionRepositoryImpl
import me.shadykhalifa.whispertop.data.services.AudioRecorderServiceImpl
import me.shadykhalifa.whispertop.data.services.FileReaderServiceImpl
import me.shadykhalifa.whispertop.domain.repositories.AudioRepository
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.domain.usecases.StartRecordingUseCase
import me.shadykhalifa.whispertop.domain.usecases.StopRecordingUseCase
import me.shadykhalifa.whispertop.presentation.viewmodels.RecordingViewModel
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
    
    // Repositories - Now depend on service interfaces instead of platform-specific implementations
    singleOf(::SettingsRepositoryImpl) { bind<SettingsRepository>() }
    singleOf(::AudioRepositoryImpl) { bind<AudioRepository>() }
    singleOf(::TranscriptionRepositoryImpl) { bind<TranscriptionRepository>() }
    
    // Use Cases
    factoryOf(::StartRecordingUseCase)
    factoryOf(::StopRecordingUseCase)
    
    // ViewModels
    singleOf(::RecordingViewModel)
}