package me.shadykhalifa.whispertop

import android.app.Application

class TestApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Minimal test application to avoid Koin conflicts
    }
}