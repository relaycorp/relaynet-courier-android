package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.flow.map
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateReceiver
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class HotspotInstructionsViewModel
@Inject constructor(
    private val wifiHotspotStateReceiver: WifiHotspotStateReceiver
) : BaseViewModel() {

    fun state() =
        wifiHotspotStateReceiver
            .state()
            .map { it.toState() }

    private fun WifiHotspotState.toState() =
        when (this) {
            WifiHotspotState.Enabled -> State.ReadyToSync
            WifiHotspotState.Disabled -> State.NotReadyToSync
        }

    enum class State {
        NotReadyToSync, ReadyToSync
    }
}
