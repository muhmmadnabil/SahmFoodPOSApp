package com.sahm.pos.domain.sync

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

interface NetworkMonitor {
    val networkStatus: Flow<NetworkStatus>
    val isOnline: Flow<Boolean>
        get() = networkStatus
            .map { it.isOnline }
            .distinctUntilChanged()
}

data class NetworkStatus(
    val isOnline: Boolean,
    val networkId: String? = null,
)
