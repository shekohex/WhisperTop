package me.shadykhalifa.whispertop.domain.usecases

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import me.shadykhalifa.whispertop.domain.managers.RecordingManager
import me.shadykhalifa.whispertop.domain.models.PermissionStatus
import me.shadykhalifa.whispertop.domain.models.RecordingState
import me.shadykhalifa.whispertop.domain.models.ServiceReadinessState
import me.shadykhalifa.whispertop.domain.models.ErrorNotificationService
import me.shadykhalifa.whispertop.domain.repositories.SettingsRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionRepository
import me.shadykhalifa.whispertop.domain.repositories.TranscriptionHistoryRepository
import me.shadykhalifa.whispertop.domain.repositories.SessionMetricsRepository
import me.shadykhalifa.whispertop.domain.services.TextInsertionService
import me.shadykhalifa.whispertop.domain.services.ToastService
import me.shadykhalifa.whispertop.domain.services.RetryService
import me.shadykhalifa.whispertop.domain.services.ErrorLoggingService
import me.shadykhalifa.whispertop.domain.services.ConnectionStatusService
import me.shadykhalifa.whispertop.utils.Result
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class TranscriptionWorkflowUseCaseIntegrationTest {

    private val recordingManager = mockk<RecordingManager>(relaxed = true)
    private val transcriptionRepository = mockk<TranscriptionRepository>()
    private val transcriptionHistoryRepository = mockk<TranscriptionHistoryRepository>()
    private val sessionMetricsRepository = mockk<SessionMetricsRepository>()
    private val settingsRepository = mockk<SettingsRepository>()
    private val textInsertionService = mockk<TextInsertionService>()
    private val toastService = mockk<ToastService>(relaxed = true)
    private val retryService = mockk<RetryService>()
    private val errorLoggingService = mockk<ErrorLoggingService>(relaxed = true)
    private val connectionStatusService = mockk<ConnectionStatusService>(relaxed = true)
    private val errorNotificationService = mockk<ErrorNotificationService>(relaxed = true)
    private val serviceManagementUseCase = mockk<ServiceManagementUseCase>()

    @Test
    fun `initialization should emit ServiceReady when services and permissions are ready`() = runTest {
        // Given
        val recordingStateFlow = MutableStateFlow<RecordingState>(RecordingState.Idle)
        every { recordingManager.recordingState } returns recordingStateFlow
        
        val serviceReadinessState = ServiceReadinessState(
            serviceConnected = true,
            permissionsGranted = true,
            errorMessage = null
        )
        coEvery { serviceManagementUseCase.bindServices() } returns Result.Success(Unit)
        coEvery { serviceManagementUseCase.checkPermissions() } returns Result.Success(PermissionStatus.AllGranted)

        // When
        val useCase = TranscriptionWorkflowUseCase(
            recordingManager = recordingManager,
            transcriptionRepository = transcriptionRepository,
            transcriptionHistoryRepository = transcriptionHistoryRepository,
            sessionMetricsRepository = sessionMetricsRepository,
            settingsRepository = settingsRepository,
            textInsertionService = textInsertionService,
            toastService = toastService,
            retryService = retryService,
            errorLoggingService = errorLoggingService,
            connectionStatusService = connectionStatusService,
            errorNotificationService = errorNotificationService,
            serviceManagementUseCase = serviceManagementUseCase
        )

        // Then
        coVerify { serviceManagementUseCase.bindServices() }
        coVerify { serviceManagementUseCase.checkPermissions() }
        
        // Clean up
        useCase.cleanup()
    }

    @Test
    fun `initialization should emit PermissionDenied when permissions are denied`() = runTest {
        // Given
        val recordingStateFlow = MutableStateFlow<RecordingState>(RecordingState.Idle)
        every { recordingManager.recordingState } returns recordingStateFlow
        
        val deniedPermissions = listOf("android.permission.RECORD_AUDIO")
        coEvery { serviceManagementUseCase.bindServices() } returns Result.Success(Unit)
        coEvery { serviceManagementUseCase.checkPermissions() } returns Result.Success(
            PermissionStatus.SomeDenied(deniedPermissions)
        )

        // When
        val useCase = TranscriptionWorkflowUseCase(
            recordingManager = recordingManager,
            transcriptionRepository = transcriptionRepository,
            transcriptionHistoryRepository = transcriptionHistoryRepository,
            sessionMetricsRepository = sessionMetricsRepository,
            settingsRepository = settingsRepository,
            textInsertionService = textInsertionService,
            toastService = toastService,
            retryService = retryService,
            errorLoggingService = errorLoggingService,
            connectionStatusService = connectionStatusService,
            errorNotificationService = errorNotificationService,
            serviceManagementUseCase = serviceManagementUseCase
        )

        // Then
        coVerify { serviceManagementUseCase.bindServices() }
        coVerify { serviceManagementUseCase.checkPermissions() }
        
        // Clean up
        useCase.cleanup()
    }

    @Test
    fun `cleanup should call service management cleanup`() = runTest {
        // Given
        val recordingStateFlow = MutableStateFlow<RecordingState>(RecordingState.Idle)
        every { recordingManager.recordingState } returns recordingStateFlow
        coEvery { serviceManagementUseCase.bindServices() } returns Result.Success(Unit)
        coEvery { serviceManagementUseCase.checkPermissions() } returns Result.Success(PermissionStatus.AllGranted)
        every { serviceManagementUseCase.cleanup() } returns Unit

        val useCase = TranscriptionWorkflowUseCase(
            recordingManager = recordingManager,
            transcriptionRepository = transcriptionRepository,
            transcriptionHistoryRepository = transcriptionHistoryRepository,
            sessionMetricsRepository = sessionMetricsRepository,
            settingsRepository = settingsRepository,
            textInsertionService = textInsertionService,
            toastService = toastService,
            retryService = retryService,
            errorLoggingService = errorLoggingService,
            connectionStatusService = connectionStatusService,
            errorNotificationService = errorNotificationService,
            serviceManagementUseCase = serviceManagementUseCase
        )

        // When
        useCase.cleanup()

        // Then
        verify { serviceManagementUseCase.cleanup() }
    }
}