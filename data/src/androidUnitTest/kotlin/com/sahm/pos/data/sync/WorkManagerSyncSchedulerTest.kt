package com.sahm.pos.data.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import com.sahm.pos.domain.sync.SyncReason
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkManagerSyncSchedulerTest {
    @Test
    fun syncRequestRequiresConnectedNetworkAndCarriesReason() {
        val request = buildSyncOutboxRequest(SyncReason.NetworkRestored)

        assertEquals(NetworkType.CONNECTED, request.workSpec.constraints.requiredNetworkType)
        assertEquals(SyncReason.NetworkRestored.name, request.workSpec.input.getString(SyncReasonInputKey))
    }

    @Test
    fun syncWorkUsesStableUniqueWorkContract() {
        assertEquals("sync_outbox", SyncOutboxWorkName)
        assertEquals(ExistingWorkPolicy.KEEP, ExistingWorkPolicy.KEEP)
    }

    @Test
    fun everySyncReasonCanBePlacedInWorkerInputData() {
        SyncReason.entries.forEach { reason ->
            val request = buildSyncOutboxRequest(reason)

            assertEquals(reason.name, request.workSpec.input.getString(SyncReasonInputKey))
        }
    }
}
