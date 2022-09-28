package tech.relaycorp.courier.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.cogrpc.server.GatewayIPAddressException
import tech.relaycorp.cogrpc.server.Networking
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.common.tickerFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class WifiHotspotStateReceiver
@Inject constructor(
    private val context: Context
) {

    private val state = BehaviorChannel(WifiHotspotState.Disabled)
    fun state() = state.asFlow().distinctUntilChanged()

    private val coroutineContext = SupervisorJob() + Dispatchers.IO
    private var pollingGatewayAddressesJob: Job? = null

    fun register() {
        if (isWifiApStateChangeAvailable) {
            context.registerReceiver(
                wifiApStateChangeReceiver,
                IntentFilter(WIFI_AP_STATE_CHANGED_ACTION)
            )
        } else {
            startPollingGatewayAddresses()
        }
    }

    fun unregister() {
        if (isWifiApStateChangeAvailable) {
            context.unregisterReceiver(wifiApStateChangeReceiver)
        } else {
            stopPollingGatewayAddresses()
        }
    }

    private fun startPollingGatewayAddresses() {
        pollingGatewayAddressesJob = tickerFlow(delayDuration)
            .map {
                try {
                    Networking.getGatewayIpAddress()
                    WifiHotspotState.Enabled
                } catch (exception: GatewayIPAddressException) {
                    WifiHotspotState.Disabled
                }
            }
            .distinctUntilChanged { oldWifiHotspotState, newWifiHotspotState ->
                oldWifiHotspotState == newWifiHotspotState
            }
            .onEach {
                logger.info("Hotspot State $it")
                state.send(it)
            }
            .launchIn(CoroutineScope(coroutineContext))
    }

    private fun stopPollingGatewayAddresses() {
        pollingGatewayAddressesJob?.cancel()
    }

    private val wifiApStateChangeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != WIFI_AP_STATE_CHANGED_ACTION) return

            val stateFlag = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
            logger.info("Wifi State $stateFlag")
            state.trySendBlocking(
                if (stateFlag == WIFI_AP_STATE_ENABLED) {
                    WifiHotspotState.Enabled
                } else {
                    WifiHotspotState.Disabled
                }
            )
        }
    }

    companion object {
        // From WifiManager documentation
        private const val WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val WIFI_AP_STATE_ENABLED = 13
    }

    private val isWifiApStateChangeAvailable =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU

    private val delayDuration = 2.seconds
}
