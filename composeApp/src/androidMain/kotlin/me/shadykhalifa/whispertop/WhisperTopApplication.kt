package me.shadykhalifa.whispertop

import android.app.Application
import me.shadykhalifa.whispertop.di.androidAppModule
import me.shadykhalifa.whispertop.di.initKoin
import me.shadykhalifa.whispertop.di.providePlatformModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WhisperTopApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        initKoin(listOf(providePlatformModule(this), androidAppModule)) {
            androidLogger()
            androidContext(this@WhisperTopApplication)
        }
    }
}