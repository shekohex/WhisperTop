package me.shadykhalifa.whispertop.managers

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.di.androidAppModule
import me.shadykhalifa.whispertop.di.providePlatformModule
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class AudioServiceManagerTest : KoinTest {
    
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    
    private val audioServiceManager: AudioServiceManager by inject()
    
    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        
        startKoin {
            androidContext(context)
            modules(providePlatformModule(context), androidAppModule)
        }
    }
    
    @After
    fun tearDown() {
        stopKoin()
    }
    
    @Test
    fun testInitialState() = testScope.runTest {
        assertEquals(
            AudioServiceManager.ServiceConnectionState.DISCONNECTED,
            audioServiceManager.connectionState.first()
        )
    }
    
    @Test
    fun testServiceManagerCreation() {
        assertNotNull(audioServiceManager)
    }
    
    @Test
    fun testRecordingActionWhenNotBound() {
        val result = audioServiceManager.startRecording()
        assertEquals(AudioServiceManager.RecordingActionResult.SERVICE_NOT_BOUND, result)
    }
    
    @Test
    fun testCleanupDoesNotThrow() {
        audioServiceManager.cleanup()
        // Should not throw any exceptions
        assertTrue(true)
    }
}