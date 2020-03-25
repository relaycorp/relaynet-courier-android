package tech.relaycorp.courier.background

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InternetConnectionObserver
@Inject constructor(
    connectivityManager: ConnectivityManager
) {

    internal val state = ConflatedBroadcastChannel(InternetConnection.Offline)

    private val networkRequest =
        NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            state.sendBlocking(InternetConnection.Online)
        }

        override fun onUnavailable() {
            state.sendBlocking(InternetConnection.Offline)
        }

        override fun onLost(network: Network) {
            state.sendBlocking(InternetConnection.Offline)
        }
    }

    init {
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }

    fun observe() = state.asFlow().distinctUntilChanged()
}
