package tech.relaycorp.courier.background

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InternetConnectionObserver
    @Inject
    constructor(
        connectivityManager: ConnectivityManager,
    ) {
        internal val state = MutableStateFlow(InternetConnection.Offline)

        private val networkRequest =
            NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

        private val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    state.value = InternetConnection.Online
                }

                override fun onUnavailable() {
                    state.value = InternetConnection.Offline
                }

                override fun onLost(network: Network) {
                    state.value = InternetConnection.Offline
                }
            }

        init {
            connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        }

        fun observe(): Flow<InternetConnection> = state.asStateFlow()
    }
