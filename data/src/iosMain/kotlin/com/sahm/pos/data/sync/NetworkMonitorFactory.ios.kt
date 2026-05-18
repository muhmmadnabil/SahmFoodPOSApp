@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.sahm.pos.data.sync

import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.sync.NetworkMonitor
import com.sahm.pos.domain.sync.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import platform.Network.nw_path_get_status
import platform.Network.nw_path_monitor_cancel
import platform.Network.nw_path_monitor_create
import platform.Network.nw_path_monitor_set_queue
import platform.Network.nw_path_monitor_set_update_handler
import platform.Network.nw_path_monitor_start
import platform.Network.nw_path_status_satisfied
import platform.darwin.dispatch_get_main_queue

actual fun createNetworkMonitor(platformContext: PlatformContext): NetworkMonitor =
    IosNetworkMonitor()

/**
 * iOS NetworkMonitor backed by the system Network.framework path monitor.
 * Emits the current connectivity state whenever it changes, and on start.
 * Matches Android behavior: emits true only when validated internet is available.
 */
internal class IosNetworkMonitor : NetworkMonitor {
    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        val monitor = nw_path_monitor_create()

        nw_path_monitor_set_update_handler(monitor) { path ->
            val isOnline = path != null && nw_path_get_status(path) == nw_path_status_satisfied
            trySend(NetworkStatus(isOnline = isOnline))
        }

        nw_path_monitor_set_queue(monitor, dispatch_get_main_queue())
        nw_path_monitor_start(monitor)

        awaitClose {
            nw_path_monitor_cancel(monitor)
        }
    }.distinctUntilChanged()
}
