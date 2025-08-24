package me.shadykhalifa.whispertop.domain.services

import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.models.TranscriptionSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PerformanceCacheManagerTest {
    
    private fun createTestSession(id: String): TranscriptionSession {
        return TranscriptionSession(
            id = id,
            audioFilePath = "/test/$id.wav",
            transcriptionText = "Test transcription $id",
            timestamp = System.currentTimeMillis(),
            duration = 5000L,
            language = "en",
            model = "whisper-1"
        )
    }
    
    @Test
    fun `put and get should store and retrieve items correctly`() = runTest {
        val cache = PerformanceCacheManagerImpl(maxSize = 5)
        val session = createTestSession("test1")
        
        cache.put("key1", session)
        val retrieved = cache.get("key1")
        
        assertEquals(session, retrieved)
    }
    
    @Test
    fun `get should return null for non-existent keys`() = runTest {
        val cache = PerformanceCacheManagerImpl()
        
        val result = cache.get("non-existent")
        
        assertNull(result)
    }
    
    @Test
    fun `cache should evict LRU items when size limit exceeded`() = runTest {
        val cache = PerformanceCacheManagerImpl(maxSize = 3)
        
        // Fill cache to capacity
        cache.put("key1", createTestSession("1"))
        cache.put("key2", createTestSession("2"))
        cache.put("key3", createTestSession("3"))
        
        // Add one more item, should evict LRU
        cache.put("key4", createTestSession("4"))
        
        // First item should be evicted
        assertNull(cache.get("key1"))
        // Others should still exist
        assertNotNull(cache.get("key2"))
        assertNotNull(cache.get("key3"))
        assertNotNull(cache.get("key4"))
    }
    
    @Test
    fun `accessing item should update LRU order`() = runTest {
        val cache = PerformanceCacheManagerImpl(maxSize = 3)
        
        cache.put("key1", createTestSession("1"))
        cache.put("key2", createTestSession("2"))
        cache.put("key3", createTestSession("3"))
        
        // Access key1 to make it recently used
        cache.get("key1")
        
        // Add new item, key2 should be evicted (oldest unused)
        cache.put("key4", createTestSession("4"))
        
        assertNotNull(cache.get("key1")) // Recently accessed, should remain
        assertNull(cache.get("key2"))    // Should be evicted
        assertNotNull(cache.get("key3"))
        assertNotNull(cache.get("key4"))
    }
    
    @Test
    fun `expired items should be evicted and not returned`() = runTest {
        val shortTtl = 100L // 100ms TTL
        val cache = PerformanceCacheManagerImpl(defaultTtlMs = shortTtl)
        val session = createTestSession("expired")
        
        cache.put("key1", session)
        
        // Wait for expiration
        Thread.sleep(150)
        
        val result = cache.get("key1")
        assertNull(result)
    }
    
    @Test
    fun `evictExpired should remove expired items and return count`() = runTest {
        val shortTtl = 100L
        val cache = PerformanceCacheManagerImpl(defaultTtlMs = shortTtl)
        
        cache.put("key1", createTestSession("1"))
        cache.put("key2", createTestSession("2"))
        
        // Wait for expiration
        Thread.sleep(150)
        
        val evictedCount = cache.evictExpired()
        assertEquals(2, evictedCount)
    }
    
    @Test
    fun `remove should delete item and return it`() = runTest {
        val cache = PerformanceCacheManagerImpl()
        val session = createTestSession("remove")
        
        cache.put("key1", session)
        val removed = cache.remove("key1")
        
        assertEquals(session, removed)
        assertNull(cache.get("key1"))
    }
    
    @Test
    fun `clear should remove all items`() = runTest {
        val cache = PerformanceCacheManagerImpl()
        
        cache.put("key1", createTestSession("1"))
        cache.put("key2", createTestSession("2"))
        cache.put("key3", createTestSession("3"))
        
        cache.clear()
        
        assertNull(cache.get("key1"))
        assertNull(cache.get("key2"))
        assertNull(cache.get("key3"))
    }
    
    @Test
    fun `getStats should return accurate cache statistics`() = runTest {
        val cache = PerformanceCacheManagerImpl(maxSize = 5)
        
        // Test initial stats
        val initialStats = cache.getStats()
        assertEquals(0, initialStats.hitCount)
        assertEquals(0, initialStats.missCount)
        assertEquals(0, initialStats.currentSize)
        assertEquals(5, initialStats.maxSize)
        
        // Add some items and test
        cache.put("key1", createTestSession("1"))
        cache.put("key2", createTestSession("2"))
        
        // Hit
        cache.get("key1")
        // Miss
        cache.get("non-existent")
        
        val stats = cache.getStats()
        assertEquals(1, stats.hitCount)
        assertEquals(1, stats.missCount)
        assertEquals(2, stats.currentSize)
        assertEquals(0.5, stats.hitRate)
    }
    
    @Test
    fun `evictLRU should remove specified number of items`() = runTest {
        val cache = PerformanceCacheManagerImpl(maxSize = 5)
        
        cache.put("key1", createTestSession("1"))
        cache.put("key2", createTestSession("2"))
        cache.put("key3", createTestSession("3"))
        cache.put("key4", createTestSession("4"))
        
        val evictedKeys = cache.evictLRU(2)
        
        assertEquals(2, evictedKeys.size)
        assertTrue(evictedKeys.contains("key1"))
        assertTrue(evictedKeys.contains("key2"))
        
        // Remaining items should still exist
        assertNotNull(cache.get("key3"))
        assertNotNull(cache.get("key4"))
    }
    
    @Test
    fun `observeStats should emit updated statistics`() = runTest {
        val cache = PerformanceCacheManagerImpl()
        val statsFlow = cache.observeStats()
        
        var receivedStats: me.shadykhalifa.whispertop.domain.models.CacheStats? = null
        val job = kotlinx.coroutines.launch {
            statsFlow.collect { stats ->
                receivedStats = stats
            }
        }
        
        cache.put("key1", createTestSession("1"))
        
        // Give some time for the flow to emit
        kotlinx.coroutines.delay(100)
        
        assertNotNull(receivedStats)
        assertEquals(1, receivedStats?.currentSize)
        
        job.cancel()
    }
}