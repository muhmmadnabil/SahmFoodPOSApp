package com.sahm.pos.data.sync

import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.sync.NetworkMonitor

expect fun createNetworkMonitor(platformContext: PlatformContext): NetworkMonitor
