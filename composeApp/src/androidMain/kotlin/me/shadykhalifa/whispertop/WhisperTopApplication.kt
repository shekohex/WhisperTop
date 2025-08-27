package me.shadykhalifa.whispertop

import android.app.Application
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.shadykhalifa.whispertop.di.androidAppModule
import me.shadykhalifa.whispertop.di.initKoin
import me.shadykhalifa.whispertop.di.providePlatformModule
import me.shadykhalifa.whispertop.presentation.ui.components.setGlobalContext
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.utils.Result
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class WhisperTopApplication : Application() {

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Set global context for expect functions
        setGlobalContext(this)

        initKoin(listOf(providePlatformModule(this), androidAppModule)) {
            androidLogger()
            androidContext(this@WhisperTopApplication)
        }

        // Initialize default user statistics in background
        initializeDefaultStatistics()
    }

    private fun initializeDefaultStatistics() {
        applicationScope.launch {
            try {
                Log.d("WhisperTopApplication", "Initializing default user statistics...")

                // Get the repository from Koin
                val userStatisticsRepository: UserStatisticsRepository by inject()

                val defaultUserId = "default_user"

                // Check if statistics already exist
                val existingStats = userStatisticsRepository.getUserStatistics(defaultUserId)

                when (existingStats) {
                    is Result.Success -> {
                        if (existingStats.data == null) {
                            Log.d("WhisperTopApplication", "No statistics found, creating default statistics...")
                            val createResult = userStatisticsRepository.createUserStatistics(defaultUserId)
                            when (createResult) {
                                is Result.Success -> {
                                    Log.i("WhisperTopApplication", "Default statistics created successfully")
                                }
                                is Result.Error -> {
                                    Log.e("WhisperTopApplication", "Failed to create default statistics: ${createResult.exception.message}")
                                }
                                is Result.Loading -> {
                                    Log.d("WhisperTopApplication", "Statistics creation in progress...")
                                }
                            }
                        } else {
                            Log.d("WhisperTopApplication", "Statistics already exist, skipping initialization")
                        }
                    }
                    is Result.Error -> {
                        Log.e("WhisperTopApplication", "Error checking existing statistics: ${existingStats.exception.message}")
                        // Try to create anyway
                        val createResult = userStatisticsRepository.createUserStatistics(defaultUserId)
                        when (createResult) {
                            is Result.Success -> {
                                Log.i("WhisperTopApplication", "Default statistics created after error")
                            }
                            is Result.Error -> {
                                Log.e("WhisperTopApplication", "Failed to create default statistics after error: ${createResult.exception.message}")
                            }
                            is Result.Loading -> {
                                Log.d("WhisperTopApplication", "Statistics creation in progress after error...")
                            }
                        }
                    }
                    is Result.Loading -> {
                        Log.d("WhisperTopApplication", "Statistics check in progress...")
                    }
                }

            } catch (e: Exception) {
                Log.e("WhisperTopApplication", "Exception during statistics initialization: ${e.message}", e)
            }
        }
    }
}