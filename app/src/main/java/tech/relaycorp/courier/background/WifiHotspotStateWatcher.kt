package tech.relaycorp.courier.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.AppModule.WifiApStateAvailability
import tech.relaycorp.courier.BackgroundCoroutineContext
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.common.tickerFlow
import tech.relaycorp.courier.domain.server.GetGatewayState
import tech.relaycorp.courier.domain.server.GetGatewayState.GatewayState
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.seconds

@Singleton
class WifiHotspotStateWatcher
@Inject constructor(
    private val context: Context,
    private val wifiApState: WifiApStateAvailability,
    private val foregroundAppMonitor: ForegroundAppMonitor,
    private val gatewayIpAddress: GetGatewayState,
    @BackgroundCoroutineContext private val backgroundCoroutineContext: CoroutineContext
) {

    private val state = MutableStateFlow(WifiHotspotState.Disabled)
    fun state() = state.asStateFlow()

    private var pollingGatewayAddressesJob: Job? = null

    fun start() {
        when (wifiApState) {
            WifiApStateAvailability.Available -> {
                context.registerReceiver(
                    wifiApStateChangeReceiver,
                    IntentFilter(WIFI_AP_STATE_CHANGED_ACTION)
                )
            }
            WifiApStateAvailability.Unavailable -> {
                startPollingGatewayAddresses()
            }
        }
    }

    fun stop() {
        when (wifiApState) {
            WifiApStateAvailability.Available -> {
                context.unregisterReceiver(wifiApStateChangeReceiver)
            }
            WifiApStateAvailability.Unavailable -> {
                stopPollingGatewayAddresses()
            }
        }
    }

    private fun startPollingGatewayAddresses() {
        pollingGatewayAddressesJob = foregroundAppMonitor.observe()
            .flatMapLatest {
                if (it == ForegroundAppMonitor.State.Foreground) {
                    tickerFlow(POLLING_GATEWAY_ADDRESS_INTERVAL)
                } else {
                    emptyFlow()
                }
            }.map {
                when (gatewayIpAddress()) {
                    GatewayState.Available -> WifiHotspotState.Enabled
                    GatewayState.Unavailable -> WifiHotspotState.Disabled
                }
            }
            .distinctUntilChanged()
            .onEach {
                logger.info("Hotspot State $it")
                state.value = it
            }
            .launchIn(CoroutineScope(backgroundCoroutineContext))
    }

    private fun stopPollingGatewayAddresses() {
        pollingGatewayAddressesJob?.cancel()
        pollingGatewayAddressesJob = null
    }

    private val wifiApStateChangeReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action != WIFI_AP_STATE_CHANGED_ACTION) return

                val stateFlag = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, 0)
                logger.info("Hotspot State $stateFlag")
                state.value =
                    if (stateFlag == WIFI_AP_STATE_ENABLED) {
                        WifiHotspotState.Enabled
                    } else {
                        WifiHotspotState.Disabled
                    }
            }
        }
    }

    companion object {
        // From WifiManager documentation
        private const val WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val WIFI_AP_STATE_ENABLED = 13

        private val POLLING_GATEWAY_ADDRESS_INTERVAL = 2.seconds
    }
}
