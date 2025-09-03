package me.shadykhalifa.whispertop.data.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import me.shadykhalifa.whispertop.data.local.PreferencesDataSource
import me.shadykhalifa.whispertop.data.models.toDomain
import me.shadykhalifa.whispertop.data.models.toEntity
import me.shadykhalifa.whispertop.domain.models.AppSettings
import me.shadykhalifa.whispertop.domain.models.Theme
import me.shadykhalifa.whispertop.domain.models.ExportFormat
import me.shadykhalifa.whispertop.domain.models.ChartTimeRange
import me.shadykhalifa.whispertop.domain.models.DataPrivacyMode
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.SecurePreferencesRepository
import me.shadykhalifa.whispertop.utils.Result

class SettingsRepositoryImpl(
    private val preferencesDataSource: PreferencesDataSource,
    private val securePreferencesRepository: SecurePreferencesRepository
) : BaseRepository(), SettingsRepository {

    override val settings: Flow<AppSettings> = combine(
        preferencesDataSource.getSettingsFlow(),
        securePreferencesRepository.getDataPrivacyModeFlow()
    ) { settingsEntity, dataPrivacyModeResult ->
        when (dataPrivacyModeResult) {
            is Result.Success -> {
                settingsEntity.toDomain().copy(dataPrivacyMode = dataPrivacyModeResult.data)
            }
            is Result.Error -> {
                // Use default privacy mode on error, but still include other settings
                settingsEntity.toDomain()
            }
            is Result.Loading -> {
                // Use default privacy mode during loading
                settingsEntity.toDomain()
            }
        }
    }

    override suspend fun getSettings(): AppSettings {
        val baseSettings = preferencesDataSource.getSettings().toDomain()
        val dataPrivacyModeResult = securePreferencesRepository.getDataPrivacyMode()
        
        return when (dataPrivacyModeResult) {
            is Result.Success -> {
                baseSettings.copy(dataPrivacyMode = dataPrivacyModeResult.data)
            }
            is Result.Error -> {
                // Use default privacy mode on error
                baseSettings
            }
            is Result.Loading -> {
                // Use default privacy mode during loading
                baseSettings
            }
        }
    }