package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateReceiver
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.PublishChannel
import tech.relaycorp.courier.domain.PrivateSync
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import tech.relaycorp.courier.ui.common.Finish
import javax.inject.Inject

class PeopleSyncViewModel
@Inject constructor(
    private val privateSync: PrivateSync,
    private val wifiHotspotStateReceiver: WifiHotspotStateReceiver
) : BaseViewModel() {

    // Inputs

    fun stopClicked() = stopClicks.sendBlocking(Click)
    private val stopClicks = PublishChannel<Click>()

    // Outputs

    private val state = BehaviorChannel<PrivateSync.State>()
    fun state() = state.asFlow()

    fun clientsConnected() = privateSync.clientsConnected()

    private val openHotspotInstructions = BehaviorChannel<Unit>()
    fun openHotspotInstructions() = openHotspotInstructions.asFlow()

    private val finish = BehaviorChannel<Finish>()
    fun finish() = finish.asFlow()

    init {
        ioScope.launch {
            when (getHotspotState()) {
                WifiHotspotState.Enabled -> privateSync.startSync()
                WifiHotspotState.Disabled -> {
                    openHotspotInstructions.send(Unit)
                    finish.send(Finish)
                }
            }
        }

        privateSync
            .state()
            .onEach { state.send(it) }
            .launchIn(ioScope)

        stopClicks
            .asFlow()
            .onEach { finish.send(Finish) }
            .launchIn(ioScope)
    }

    override fun onCleared() {
        privateSync.stopSync()
        super.onCleared()
    }

    private suspend fun getHotspotState() = wifiHotspotStateReceiver.state().first()
}
