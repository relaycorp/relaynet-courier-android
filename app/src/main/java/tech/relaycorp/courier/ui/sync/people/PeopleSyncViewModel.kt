package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
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

    fun confirmStopClicked() = confirmStopClicks.sendBlocking(Click)
    private val confirmStopClicks = PublishChannel<Click>()

    // Outputs

    private val state = BehaviorChannel<State>()
    fun state() = state.asFlow()

    private val openHotspotInstructions = BehaviorChannel<Unit>()
    fun openHotspotInstructions() = openHotspotInstructions.asFlow()

    private val confirmStop = PublishChannel<Unit>()
    fun confirmStop() = confirmStop.asFlow()

    private val finish = BehaviorChannel<Finish>()
    fun finish() = finish.asFlow()

    private var hadFirstClient = false

    init {
        wifiHotspotStateReceiver
            .state()
            .take(1)
            .onEach {
                when (it) {
                    WifiHotspotState.Enabled -> privateSync.startSync()
                    WifiHotspotState.Disabled -> {
                        openHotspotInstructions.send(Unit)
                        finish.send(Finish)
                    }
                }
            }
            .launchIn(ioScope)

        wifiHotspotStateReceiver
            .state()
            .drop(1)
            .filter { it == WifiHotspotState.Disabled }
            .onEach {
                privateSync.stopSync()
                openHotspotInstructions.send(Unit)
                finish.send(Finish)
            }
            .launchIn(ioScope)

        privateSync
            .clientsConnected()
            .filter { it > 0 }
            .take(1)
            .onEach { hadFirstClient = true }
            .launchIn(ioScope)

        privateSync
            .state()
            .combine(privateSync.clientsConnected()) { syncState, clientsConnected ->
                if (syncState == PrivateSync.State.Stopped) {
                    finish.send(Finish)
                } else {
                    state.send(
                        when (syncState) {
                            PrivateSync.State.Syncing -> if (hadFirstClient) {
                                State.Syncing.HadFirstClient(clientsConnected)
                            } else {
                                State.Syncing.WaitingFirstClient
                            }
                            else -> State.Error
                        }
                    )
                }
            }
            .launchIn(ioScope)

        stopClicks
            .asFlow()
            .onEach {
                val clientsConnected =
                    (state.value as? State.Syncing.HadFirstClient)?.clientsConnected ?: 0
                if (clientsConnected > 0) {
                    confirmStop.send(Unit)
                } else {
                    finish.send(Finish)
                }
            }
            .launchIn(ioScope)

        confirmStopClicks
            .asFlow()
            .onEach { finish.send(Finish) }
            .launchIn(ioScope)
    }

    override fun onCleared() {
        privateSync.stopSync()
        super.onCleared()
    }

    private suspend fun getHotspotState() = wifiHotspotStateReceiver.state().first()

    sealed class State {
        sealed class Syncing : State() {
            object WaitingFirstClient : Syncing()
            data class HadFirstClient(val clientsConnected: Int) : Syncing()
        }

        object Error : State()
    }
}
