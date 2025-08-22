package me.shadykhalifa.whispertop.utils

import android.content.Context
import android.os.Environment
import android.os.StatFs
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.repositories.UserStatisticsRepository
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test suite for StorageManager
 * Tests storage detection, cleanup strategies, and low storage handling
 */
class StorageManagerTest {
    
    private lateinit var mockContext: Context
    private lateinit var mockSessionMetricsRepository: SessionMetricsRepository
    private lateinit var mockUserStatisticsRepository: UserStatisticsRepository
    private lateinit var storageManager: StorageManager
    private lateinit var mockStatFs: StatFs
    
    @Before
    fun setup() {
        mockContext = mockk(relaxed = true)
        mockSessionMetricsRepository = mockk(relaxed = true)
        mockUserStatisticsRepository = mockk(relaxed = true)
        mockStatFs = mockk()
        
        storageManager = StorageManager(
            mockContext,
            mockSessionMetricsRepository,
            mockUserStatisticsRepository
        )
        
        // Mock static methods
        mockkStatic(Environment::class)
        mockkStatic(StatFs::class)
        
        every { Environment.getDataDirectory() } returns File("/data")
    }
    
    @Test
    fun `getStorageInfo should return abundant storage level`() = runTest {
        // Given - 5GB total, 2GB available (abundant level)
        val blockSize = 4096L
        val totalBlocks = 1310720L // ~5GB
        val availableBlocks = 524288L // ~2GB
        val freeBlocks = 524288L
        
        every { mockStatFs.blockSizeLong } returns blockSize
        every { mockStatFs.blockCountLong } returns totalBlocks
        every { mockStatFs.availableBlocksLong } returns availableBlocks
        every { mockStatFs.freeBlocksLong } returns freeBlocks
        
        // Mock StatFs constructor
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } returns mockStatFs
        
        // When
        val storageInfo = storageManager.getStorageInfo()
        
        // Then
        assertEquals(StorageManager.StorageLevel.ABUNDANT, storageInfo.storageLevel)
        assertEquals(totalBlocks * blockSize, storageInfo.totalSpace)
        assertEquals(availableBlocks * blockSize, storageInfo.availableSpace)
        assertTrue(storageInfo.freePercentage > 0)
    }
    
    @Test
    fun `getStorageInfo should return critical storage level`() = runTest {
        // Given - 1GB total, 50MB available (critical level)
        val blockSize = 4096L
        val totalBlocks = 262144L // ~1GB
        val availableBlocks = 12800L // ~50MB
        val freeBlocks = 12800L
        
        every { mockStatFs.blockSizeLong } returns blockSize
        every { mockStatFs.blockCountLong } returns totalBlocks
        every { mockStatFs.availableBlocksLong } returns availableBlocks
        every { mockStatFs.freeBlocksLong } returns freeBlocks
        
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } returns mockStatFs
        
        // When
        val storageInfo = storageManager.getStorageInfo()
        
        // Then
        assertEquals(StorageManager.StorageLevel.CRITICAL, storageInfo.storageLevel)
    }
    
    @Test
    fun `hasSufficientStorage should return false for critical storage`() = runTest {
        // Given - very low storage
        val blockSize = 4096L
        val totalBlocks = 262144L
        val availableBlocks = 10000L // ~40MB available
        val freeBlocks = 10000L
        
        every { mockStatFs.blockSizeLong } returns blockSize
        every { mockStatFs.blockCountLong } returns totalBlocks
        every { mockStatFs.availableBlocksLong } returns availableBlocks
        every { mockStatFs.freeBlocksLong } returns freeBlocks
        
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } returns mockStatFs
        
        // When
        val hasSufficient = storageManager.hasSufficientStorage(50 * 1024 * 1024L) // Need 50MB
        
        // Then
        assertFalse(hasSufficient)
    }
    
    @Test
    fun `hasSufficientStorage should return true for abundant storage`() = runTest {
        // Given - abundant storage
        val blockSize = 4096L
        val totalBlocks = 1310720L // ~5GB
        val availableBlocks = 524288L // ~2GB
        val freeBlocks = 524288L
        
        every { mockStatFs.blockSizeLong } returns blockSize
        every { mockStatFs.blockCountLong } returns totalBlocks
        every { mockStatFs.availableBlocksLong } returns availableBlocks
        every { mockStatFs.freeBlocksLong } returns freeBlocks
        
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } returns mockStatFs
        
        // When
        val hasSufficient = storageManager.hasSufficientStorage(10 * 1024 * 1024L) // Need 10MB
        
        // Then
        assertTrue(hasSufficient)
    }
    
    @Test
    fun `performAutomatedCleanup should perform emergency cleanup for critical storage`() = runTest {
        // Given - critical storage level
        val blockSize = 4096L
        val totalBlocks = 262144L
        val availableBlocks = 12800L // ~50MB (critical)
        val freeBlocks = 12800L
        
        every { mockStatFs.blockSizeLong } returns blockSize
        every { mockStatFs.blockCountLong } returns totalBlocks
        every { mockStatFs.availableBlocksLong } returns availableBlocks
        every { mockStatFs.freeBlocksLong } returns freeBlocks
        
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } returns mockStatFs
        
        // Mock repository cleanup operations
        every { runTest { mockSessionMetricsRepository.deleteSessionsOlderThan(any()) } } returns Result.Success(50)
        every { runTest { mockSessionMetricsRepository.deleteLargeTranscriptions(any()) } } returns Result.Success(25)
        
        // Mock cache directories
        val mockCacheDir = mockk<File>()
        every { mockContext.cacheDir } returns mockCacheDir
        every { mockContext.externalCacheDir } returns null
        every { mockContext.filesDir } returns File("/data/files")
        every { mockContext.getExternalFilesDir(null) } returns null
        every { mockCacheDir.exists() } returns true
        every { mockCacheDir.listFiles() } returns emptyArray()
        
        // When
        val result = storageManager.performAutomatedCleanup()
        
        // Then
        assertTrue(result.operationsPerformed.any { it.contains("emergency") })
        assertTrue(result.recordsDeleted >= 0)
    }
    
    @Test
    fun `performAutomatedCleanup should skip cleanup for abundant storage`() = runTest {
        // Given - abundant storage level
        val blockSize = 4096L
        val totalBlocks = 1310720L // ~5GB
        val availableBlocks = 524288L // ~2GB (abundant)
        val freeBlocks = 524288L
        
        every { mockStatFs.blockSizeLong } returns blockSize
        every { mockStatFs.blockCountLong } returns totalBlocks
        every { mockStatFs.availableBlocksLong } returns availableBlocks
        every { mockStatFs.freeBlocksLong } returns freeBlocks
        
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } returns mockStatFs
        
        // When
        val result = storageManager.performAutomatedCleanup()
        
        // Then
        assertTrue(result.operationsPerformed.any { it.contains("No cleanup needed") })
        assertEquals(0, result.recordsDeleted)
        assertEquals(0L, result.bytesFreed)
    }
    
    @Test
    fun `clearDirectory should handle empty directory gracefully`() = runTest {
        // Given
        val storageManagerSpy = spyk(storageManager, recordPrivateCalls = true)
        val emptyDir = mockk<File>()
        
        every { emptyDir.exists() } returns true
        every { emptyDir.isDirectory } returns true
        every { emptyDir.listFiles() } returns emptyArray()
        
        // When - using reflection to call private method
        val clearDirectoryMethod = StorageManager::class.java.getDeclaredMethod("clearDirectory", File::class.java)
        clearDirectoryMethod.isAccessible = true
        val result = clearDirectoryMethod.invoke(storageManagerSpy, emptyDir) as Pair<Long, Int>
        
        // Then
        assertEquals(0L, result.first) // bytes freed
        assertEquals(0, result.second) // files deleted
    }
    
    @Test
    fun `formatBytes should format different sizes correctly`() = runTest {
        // Given
        val storageManagerSpy = spyk(storageManager, recordPrivateCalls = true)
        val formatBytesMethod = StorageManager::class.java.getDeclaredMethod("formatBytes", Long::class.java)
        formatBytesMethod.isAccessible = true
        
        // Test cases: bytes, KB, MB, GB
        val testCases = listOf(
            512L to "512B",
            1536L to "1KB", // 1.5KB rounds down
            1572864L to "1MB", // 1.5MB rounds down
            2147483648L to "2GB" // 2GB exactly
        )
        
        testCases.forEach { (bytes, expected) ->
            // When
            val result = formatBytesMethod.invoke(storageManagerSpy, bytes) as String
            
            // Then
            assertEquals(expected, result)
        }
    }
    
    @Test
    fun `getStorageInfo should handle StatFs exceptions gracefully`() = runTest {
        // Given - StatFs throws exception
        mockkStatic(StatFs::class)
        every { StatFs(any<String>()) } throws RuntimeException("Storage access failed")
        
        // When
        val storageInfo = storageManager.getStorageInfo()
        
        // Then - should return safe defaults
        assertEquals(StorageManager.StorageLevel.CRITICAL, storageInfo.storageLevel)
        assertEquals(0L, storageInfo.totalSpace)
        assertEquals(0L, storageInfo.availableSpace)
    }
}