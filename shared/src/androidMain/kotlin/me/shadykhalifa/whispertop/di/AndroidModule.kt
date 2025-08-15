package me.shadykhalifa.whispertop.di

import android.content.Context
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.local.PreferencesDataSourceImpl
import me.shadykhalifa.whispertop.data.repositories.AudioRecorder
import me.shadykhalifa.whispertop.data.repositories.FileReader
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val androidModule = module {
    single<PreferencesDataSource> { PreferencesDataSourceImpl(get()) }
    single<AudioRecorder> { AudioRecorder(get()) }
    single<FileReader> { FileReader() }
}

fun providePlatformModule(context: Context) = module {
    single<Context> { context }
    includes(androidModule)
}