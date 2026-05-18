package com.sahm.pos.data.sync

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.sahm.pos.data.local.PlatformContext
import com.sahm.pos.domain.sync.NetworkMonitor
import com.sahm.pos.domain.sync.NetworkStatus
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

@SuppressLint("MissingPermission")
class AndroidNetworkMonitor(
    context: Context,
) : NetworkMonitor {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    override val networkStatus: Flow<NetworkStatus> = callbackFlow {
        fun sendCurrentStatus() {
            trySend(connectivityManager.currentNetworkStatus())
        }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                sendCurrentStatus()
            }

            override fun onLost(network: Network) {
                sendCurrentStatus()
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities,
            ) {
                sendCurrentStatus()
            }
        }

        sendCurrentStatus()
        connectivityManager.registerNetworkCallback(
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build(),
            callback,
        )

        awaitClose {
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()

    private fun ConnectivityManager.currentNetworkStatus(): NetworkStatus {
        val network = activeNetwork ?: return NetworkStatus(isOnline = false)
        val capabilities = getNetworkCapabilities(network) ?: return NetworkStatus(
            isOnline = false,
            networkId = network.networkHandle.toString(),
        )
        val isOnline = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        return NetworkStatus(
            isOnline = isOnline,
            networkId = network.networkHandle.toString(),
        )
    }
}

actual fun createNetworkMonitor(platformContext: PlatformContext): NetworkMonitor =
    AndroidNetworkMonitor(platformContext.context)
