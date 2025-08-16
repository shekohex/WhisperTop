package me.shadykhalifa.whispertop.domain.services

import me.shadykhalifa.whispertop.domain.models.AudioFile

interface AudioCacheService {
    suspend fun cacheAudioFile(audioFile: AudioFile): Boolean
    suspend fun getCachedAudioFile(fileName: String): AudioFile?
    suspend fun clearExpiredCache()
    suspend fun clearAllCache()
    suspend fun getCacheSize(): Long
}