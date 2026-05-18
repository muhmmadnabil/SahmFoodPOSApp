package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.sync.NetworkMonitor
import com.sahm.pos.domain.sync.NetworkStatus
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ObserveSyncTriggersUseCaseTest {
    @Test
    fun schedulesAppStartedWhenStarted() = runTest(UnconfinedTestDispatcher()) {
        val scheduler = RecordingSyncScheduler()
        val observer = ObserveSyncTriggersUseCase(FakeNetworkMonitor(), scheduler)

        observer.start(backgroundScope)

        assertEquals(listOf(SyncReason.AppStarted), scheduler.reasons)
    }

    @Test
    fun schedulesNetworkRestoredWhenConnectivityBecomesOnline() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeNetworkMonitor()
        val scheduler = RecordingSyncScheduler()
        val observer = ObserveSyncTriggersUseCase(monitor, scheduler)

        observer.start(backgroundScope)
        monitor.emit(false)
        monitor.emit(true)

        assertEquals(
            listOf(SyncReason.AppStarted, SyncReason.NetworkRestored),
            scheduler.reasons,
        )
    }

    @Test
    fun doesNotScheduleRepeatedlyForDuplicateOnlineValues() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeNetworkMonitor()
        val scheduler = RecordingSyncScheduler()
        val observer = ObserveSyncTriggersUseCase(monitor, scheduler)

        observer.start(backgroundScope)
        monitor.emit(true)
        monitor.emit(true)

        assertEquals(
            listOf(SyncReason.AppStarted, SyncReason.NetworkRestored),
            scheduler.reasons,
        )
    }

    @Test
    fun schedulesWhenConnectedNetworkChangesWhileStillOnline() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeNetworkMonitor()
        val scheduler = RecordingSyncScheduler()
        val observer = ObserveSyncTriggersUseCase(monitor, scheduler)

        observer.start(backgroundScope)
        monitor.emit(true, networkId = "wifi")
        monitor.emit(true, networkId = "cellular")

        assertEquals(
            listOf(SyncReason.AppStarted, SyncReason.NetworkRestored, SyncReason.NetworkRestored),
            scheduler.reasons,
        )
    }

    @Test
    fun startsOnlyOneCollectorAndStopsCleanly() = runTest(UnconfinedTestDispatcher()) {
        val monitor = FakeNetworkMonitor()
        val scheduler = RecordingSyncScheduler()
        val observer = ObserveSyncTriggersUseCase(monitor, scheduler)

        observer.start(backgroundScope)
        observer.start(backgroundScope)
        monitor.emit(true)
        observer.stop()
        monitor.emit(false)
        monitor.emit(true)

        assertEquals(
            listOf(SyncReason.AppStarted, SyncReason.NetworkRestored),
            scheduler.reasons,
        )
    }
}

private class FakeNetworkMonitor : NetworkMonitor {
    private val status = MutableSharedFlow<NetworkStatus>(extraBufferCapacity = 8)
    override val networkStatus: Flow<NetworkStatus> = status

    fun emit(value: Boolean, networkId: String? = null) {
        status.tryEmit(NetworkStatus(isOnline = value, networkId = networkId))
    }
}

private class RecordingSyncScheduler : SyncScheduler {
    val reasons = mutableListOf<SyncReason>()

    override fun scheduleSync(reason: SyncReason) {
        reasons += reason
    }
}
