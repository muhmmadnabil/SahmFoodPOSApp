package com.sahm.pos.domain.usecase

import com.sahm.pos.domain.sync.NetworkMonitor
import com.sahm.pos.domain.sync.SyncReason
import com.sahm.pos.domain.sync.SyncScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class ObserveSyncTriggersUseCase(
    private val networkMonitor: NetworkMonitor,
    private val syncScheduler: SyncScheduler,
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return

        runCatching { syncScheduler.scheduleSync(SyncReason.AppStarted) }
        job = networkMonitor.networkStatus
            .distinctUntilChanged()
            .filter { it.isOnline }
            .onEach {
                syncScheduler.scheduleSync(SyncReason.NetworkRestored)
            }
            .launchIn(scope)
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
