package tech.relaycorp.courier.background

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.Logging.logger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WifiHotspotStateReceiver
@Inject constructor(
    private val context: Context
) : BroadcastReceiver() {

    private val state = BehaviorChannel(WifiHotspotState.Disabled)
    fun state() = state.asFlow().distinctUntilChanged()

    fun register() {
        context.registerReceiver(this, IntentFilter(WIFI_AP_STATE_CHANGED_ACTION))
    }

    fun unregister() {
        context.unregisterReceiver(this)
    }

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

    companion object {
        // From WifiManager documentation
        private const val WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED"
        private const val WIFI_AP_STATE_ENABLED = 13
    }
}
