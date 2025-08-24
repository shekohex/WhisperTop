package me.shadykhalifa.whispertop.di

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Clock
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.AppSettingsEntity
import me.shadykhalifa.whispertop.data.models.AudioFileEntity
import me.shadykhalifa.whispertop.domain.models.*
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.StatisticsRepository
import me.shadykhalifa.whispertop.domain.services.AudioRecorderService
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import me.shadykhalifa.whispertop.domain.services.MemoryProfiler
import me.shadykhalifa.whispertop.utils.Result
import org.koin.dsl.module

class MockPreferencesDataSource : PreferencesDataSource {
    private val _settingsFlow = MutableStateFlow(AppSettingsEntity())
    
    override suspend fun getSettings(): AppSettingsEntity = AppSettingsEntity()
    
    override suspend fun saveSettings(settings: AppSettingsEntity) {
        _settingsFlow.value = settings
    }
    
    override fun getSettingsFlow(): Flow<AppSettingsEntity> = _settingsFlow
    
    override suspend fun getLastRecording(): AudioFileEntity? = null
    
    override suspend fun saveLastRecording(audioFile: AudioFileEntity) {}
    
    override suspend fun clearLastRecording() {}
}

class MockAudioRecorderService : AudioRecorderService {
    override suspend fun startRecording() {}
    
    override suspend fun stopRecording(): AudioFile = AudioFile(
        path = "/mock/path/recording.wav",
        durationMs = 10000L,
        sizeBytes = 1024L
    )
    
    override suspend fun cancelRecording() {}
}

class MockFileReaderService : FileReaderService {
    override suspend fun readFileAsBytes(filePath: String): ByteArray = byteArrayOf(1, 2, 3, 4, 5)
}

class MockSecurePreferencesRepository : SecurePreferencesRepository {
    private var apiKey: String? = null
    private var apiEndpoint: String = "https://api.openai.com/v1"
    
    override suspend fun saveApiKey(apiKey: String): Result<Unit> {
        this.apiKey = apiKey
        return Result.Success(Unit)
    }
    
    override suspend fun getApiKey(): Result<String?> {
        return Result.Success(apiKey)
    }
    
    override suspend fun clearApiKey(): Result<Unit> {
        this.apiKey = null
        return Result.Success(Unit)
    }
    
    override suspend fun hasApiKey(): Result<Boolean> {
        return Result.Success(apiKey != null)
    }
    
    override suspend fun saveApiEndpoint(endpoint: String): Result<Unit> {
        this.apiEndpoint = endpoint
        return Result.Success(Unit)
    }
    
    override suspend fun getApiEndpoint(): Result<String> {
        return Result.Success(apiEndpoint)
    }
    
    override fun validateApiKey(apiKey: String, isOpenAIEndpoint: Boolean): Boolean {
        if (!isOpenAIEndpoint) {
            // Custom endpoints allow empty API keys or basic validation
            return apiKey.isBlank() || apiKey.length >= 3
        }
        return apiKey.isNotBlank() && apiKey.length > 10
    }

    private var wpm: Int = 60
    private var wpmOnboardingCompleted: Boolean = false

    override suspend fun saveWpm(wpm: Int): Result<Unit> {
        this.wpm = wpm
        return Result.Success(Unit)
    }

    override suspend fun getWpm(): Result<Int> {
        return Result.Success(wpm)
    }

    override suspend fun saveWpmOnboardingCompleted(completed: Boolean): Result<Unit> {
        this.wpmOnboardingCompleted = completed
        return Result.Success(Unit)
    }

    override suspend fun isWpmOnboardingCompleted(): Result<Boolean> {
        return Result.Success(wpmOnboardingCompleted)
    }

    override fun validateWpm(wpm: Int): Boolean {
        return wpm in 1..300 // Reasonable WPM range
    }

    // Statistics preferences
    private var statisticsEnabled: Boolean = true
    private var historyRetentionDays: Int = 30
    private var exportFormat: ExportFormat = ExportFormat.JSON
    private var dashboardMetricsVisible: Set<String> = DefaultDashboardMetrics.ESSENTIAL_METRICS
    private var chartTimeRange: ChartTimeRange = ChartTimeRange.DAYS_14
    private var notificationsEnabled: Boolean = true
    private var dataPrivacyMode: DataPrivacyMode = DataPrivacyMode.FULL
    private var allowDataImport: Boolean = true

    override suspend fun saveStatisticsEnabled(enabled: Boolean): Result<Unit> {
        this.statisticsEnabled = enabled
        return Result.Success(Unit)
    }

    override suspend fun getStatisticsEnabled(): Result<Boolean> {
        return Result.Success(statisticsEnabled)
    }

    override suspend fun saveHistoryRetentionDays(days: Int): Result<Unit> {
        this.historyRetentionDays = days
        return Result.Success(Unit)
    }

    override suspend fun getHistoryRetentionDays(): Result<Int> {
        return Result.Success(historyRetentionDays)
    }

    override suspend fun saveExportFormat(format: ExportFormat): Result<Unit> {
        this.exportFormat = format
        return Result.Success(Unit)
    }

    override suspend fun getExportFormat(): Result<ExportFormat> {
        return Result.Success(exportFormat)
    }

    override suspend fun saveDashboardMetricsVisible(metrics: Set<String>): Result<Unit> {
        this.dashboardMetricsVisible = metrics
        return Result.Success(Unit)
    }

    override suspend fun getDashboardMetricsVisible(): Result<Set<String>> {
        return Result.Success(dashboardMetricsVisible)
    }

    override suspend fun saveChartTimeRange(range: ChartTimeRange): Result<Unit> {
        this.chartTimeRange = range
        return Result.Success(Unit)
    }

    override suspend fun getChartTimeRange(): Result<ChartTimeRange> {
        return Result.Success(chartTimeRange)
    }

    override suspend fun saveNotificationsEnabled(enabled: Boolean): Result<Unit> {
        this.notificationsEnabled = enabled
        return Result.Success(Unit)
    }

    override suspend fun getNotificationsEnabled(): Result<Boolean> {
        return Result.Success(notificationsEnabled)
    }

    override suspend fun saveDataPrivacyMode(mode: DataPrivacyMode): Result<Unit> {
        this.dataPrivacyMode = mode
        return Result.Success(Unit)
    }

    override suspend fun getDataPrivacyMode(): Result<DataPrivacyMode> {
        return Result.Success(dataPrivacyMode)
    }

    override suspend fun saveAllowDataImport(allow: Boolean): Result<Unit> {
        this.allowDataImport = allow
        return Result.Success(Unit)
    }

    override suspend fun getAllowDataImport(): Result<Boolean> {
        return Result.Success(allowDataImport)
    }

    override fun validateHistoryRetentionDays(days: Int): Boolean {
        return days in 1..365 // 1 day to 1 year
    }
}

class MockMemoryProfiler : MemoryProfiler {
    override fun getCurrentMemoryInfo(): MemoryInfo = MemoryInfo(
        totalMemory = 1024L * 1024L * 100L, // 100MB
        freeMemory = 1024L * 1024L * 99L, // 99MB
        usedMemory = 1024L * 1024L, // 1MB
        maxMemory = 1024L * 1024L * 100L // 100MB
    )
    
    override fun trackObject(obj: Any, tag: String): String = "mock-id-${obj.hashCode()}"
    override fun releaseObject(id: String) {}
    override fun performWeakReferenceCleanup(): Int = 0
    override fun detectPotentialLeaks(): List<LeakSuspicion> = emptyList()
    override fun observeMemoryInfo(): Flow<MemoryInfo> = flowOf(getCurrentMemoryInfo())
    override fun observeLeakSuspicions(): Flow<List<LeakSuspicion>> = flowOf(emptyList())
    override fun getWeakReferenceInfo(): List<WeakReferenceInfo> = emptyList()
    override fun suggestGarbageCollection() {}
}

val mockPlatformModule = module {
    single<PreferencesDataSource> { MockPreferencesDataSource() }
    single<AudioRecorderService> { MockAudioRecorderService() }
    single<FileReaderService> { MockFileReaderService() }
    single<SecurePreferencesRepository> { MockSecurePreferencesRepository() }
    single<MemoryProfiler> { MockMemoryProfiler() }
    // Complex repository mocks removed - tests should use mockk() directly
}