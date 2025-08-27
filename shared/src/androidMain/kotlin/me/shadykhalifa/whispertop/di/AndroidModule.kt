package me.shadykhalifa.whispertop.di

import android.content.Context
import me.shadykhalifa.whispertop.data.audio.AudioRecorderImpl
import me.shadykhalifa.whispertop.data.database.AppDatabase
import me.shadykhalifa.whispertop.data.database.createDatabaseBuilder
import me.shadykhalifa.whispertop.data.database.dao.TranscriptionHistoryDao
import me.shadykhalifa.whispertop.data.database.dao.UserStatisticsDao
import me.shadykhalifa.whispertop.data.database.dao.SessionMetricsDao
import me.shadykhalifa.whispertop.data.repositories.TranscriptionHistoryRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.UserStatisticsRepositoryImpl
import me.shadykhalifa.whispertop.data.repositories.SessionMetricsRepositoryImpl
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.local.PreferencesDataSourceImpl
import me.shadykhalifa.whispertop.data.repositories.AudioRecorder
import me.shadykhalifa.whispertop.data.repositories.FileReader
import me.shadykhalifa.whispertop.data.security.SecurePreferencesRepositoryImpl
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.data.local.DatabaseKeyManager
import me.shadykhalifa.whispertop.domain.services.MemoryProfiler
import me.shadykhalifa.whispertop.domain.services.MemoryProfilerImpl
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val androidModule = module {
    single<PreferencesDataSource> { PreferencesDataSourceImpl(get()) }
    single<AudioRecorder> { AudioRecorder(get()) }
    single<AudioRecorderImpl> { AudioRecorderImpl() }
    single<FileReader> { FileReader() }
    single<SecurePreferencesRepository> { SecurePreferencesRepositoryImpl(get()) }
    
    // Database encryption
    single<DatabaseKeyManager> { DatabaseKeyManager.getInstance(get()) }
    
    // Database
    single<AppDatabase> { createDatabaseBuilder(get()) }
    single<TranscriptionHistoryDao> { get<AppDatabase>().transcriptionHistoryDao() }
    single<UserStatisticsDao> { get<AppDatabase>().userStatisticsDao() }
    single<SessionMetricsDao> { get<AppDatabase>().sessionMetricsDao() }
    
    // Room-based repositories
    single<TranscriptionHistoryRepository> { TranscriptionHistoryRepositoryImpl(get(), get()) }
    single<UserStatisticsRepository> { UserStatisticsRepositoryImpl(get(), get()) }
    single<SessionMetricsRepository> { SessionMetricsRepositoryImpl(get()) }
    single<me.shadykhalifa.whispertop.domain.services.DataRetentionService> { 
        me.shadykhalifa.whispertop.domain.services.DataRetentionServiceImpl(get(), get()) 
    }
    
    // Performance services
    single<MemoryProfiler> { MemoryProfilerImpl(get()) }
}

fun providePlatformModule(context: Context) = module {
    single<Context> { context }
    includes(androidModule)
}