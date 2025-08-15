package me.shadykhalifa.whispertop.di

import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.local.PreferencesDataSourceImpl
import me.shadykhalifa.whispertop.data.repositories.AudioRecorder
import me.shadykhalifa.whispertop.data.repositories.FileReader
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val iosModule = module {
    single<PreferencesDataSource> { PreferencesDataSourceImpl() }
    single<AudioRecorder> { AudioRecorder() }
    single<FileReader> { FileReader() }
}

fun providePlatformModule() = module {
    includes(iosModule)
}