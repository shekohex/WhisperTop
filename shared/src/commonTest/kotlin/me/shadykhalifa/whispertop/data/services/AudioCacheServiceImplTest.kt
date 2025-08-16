package me.shadykhalifa.whispertop.data.services

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.AudioFile
import me.shadykhalifa.whispertop.domain.services.FileReaderService
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AudioCacheServiceImplTest {
    
    private val mockFileReaderService = MockFileReaderService()
    private val cacheService = AudioCacheServiceImpl(mockFileReaderService)
    
    @Test
    fun `cacheAudioFile should return true for normal sized file`() = runTest {
        // Given
        val audioFile = AudioFile(
            path = "/test/audio.wav",
            durationMs = 5000,
            sizeBytes = 1024 * 1024 // 1MB
        )
        
        // When
        val result = cacheService.cacheAudioFile(audioFile)
        
        // Then
        assertTrue(result)
    }
    
    @Test
    fun `getCacheSize should return 0 when no cache files exist`() = runTest {
        // When
        val cacheSize = cacheService.getCacheSize()
        
        // Then
        assertEquals(0L, cacheSize)
    }
    
    @Test
    fun `clearAllCache should complete without error`() = runTest {
        // When
        cacheService.clearAllCache()
        
        // Then
        // Should complete without throwing exception
    }
    
    @Test
    fun `clearExpiredCache should complete without error`() = runTest {
        // When
        cacheService.clearExpiredCache()
        
        // Then
        // Should complete without throwing exception
    }
    
    @Test
    fun `getCachedAudioFile should return null for non-existent file`() = runTest {
        // Given
        val fileName = "/non/existent/file.wav"
        
        // When
        val result = cacheService.getCachedAudioFile(fileName)
        
        // Then
        assertNull(result)
    }
    
    private class MockFileReaderService : FileReaderService {
        override suspend fun readFileAsBytes(filePath: String): ByteArray {
            return ByteArray(1024) // Return 1KB of dummy data
        }
    }
}