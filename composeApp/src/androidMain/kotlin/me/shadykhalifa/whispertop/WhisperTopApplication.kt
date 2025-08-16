package me.shadykhalifa.whispertop

import android.app.Application
import me.shadykhalifa.whispertop.di.androidAppModule
import me.shadykhalifa.whispertop.di.initKoin
import me.shadykhalifa.whispertop.di.providePlatformModule
import me.shadykhalifa.whispertop.presentation.ui.components.setGlobalContext
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WhisperTopApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Set global context for expect functions
        setGlobalContext(this)
        
        initKoin(listOf(providePlatformModule(this), androidAppModule)) {
            androidLogger()
            androidContext(this@WhisperTopApplication)
        }
    }
}