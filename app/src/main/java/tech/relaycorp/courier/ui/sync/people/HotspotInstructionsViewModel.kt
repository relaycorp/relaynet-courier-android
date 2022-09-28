package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.flow.map
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateWatcher
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class HotspotInstructionsViewModel
@Inject constructor(
    private val wifiHotspotStateWatcher: WifiHotspotStateWatcher
) : BaseViewModel() {

    fun state() =
        wifiHotspotStateWatcher
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
