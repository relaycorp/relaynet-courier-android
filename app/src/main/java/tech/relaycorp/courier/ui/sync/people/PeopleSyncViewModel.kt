package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
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
    wifiHotspotStateReceiver: WifiHotspotStateReceiver
) : BaseViewModel() {

    // Inputs

    fun stopClicked() = stopClicks.trySendBlocking(Click)
    private val stopClicks = PublishChannel<Click>()

    fun confirmStopClicked() = confirmStopClicks.trySendBlocking(Click)
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
            .launchIn(scope)

        wifiHotspotStateReceiver
            .state()
            .drop(1)
            .filter { it == WifiHotspotState.Disabled }
            .onEach {
                privateSync.stopSync()
                openHotspotInstructions.send(Unit)
                finish.send(Finish)
            }
            .launchIn(scope)

        privateSync
            .clientsConnected()
            .filter { it > 0 }
            .take(1)
            .onEach { hadFirstClient = true }
            .launchIn(scope)

        combine(
            privateSync.state(),
            privateSync.clientsConnected()
        ) { syncState, clientsConnected ->
            when (syncState) {
                PrivateSync.State.Starting -> state.send(State.Starting)
                PrivateSync.State.Syncing -> state.send(
                    if (hadFirstClient) {
                        State.Syncing.HadFirstClient(clientsConnected)
                    } else {
                        State.Syncing.WaitingFirstClient
                    }
                )
                PrivateSync.State.Stopped -> finish.send(Finish)
                PrivateSync.State.Error -> state.send(State.Error)
            }
        }
            .launchIn(scope)

        stopClicks
            .asFlow()
            .onEach {
                if (state.value == State.Starting) return@onEach

                val clientsConnected =
                    (state.value as? State.Syncing.HadFirstClient)?.clientsConnected ?: 0
                if (clientsConnected > 0) {
                    confirmStop.send(Unit)
                } else {
                    finish.send(Finish)
                }
            }
            .launchIn(scope)

        confirmStopClicks
            .asFlow()
            .onEach { finish.send(Finish) }
            .launchIn(scope)
    }

    override fun onCleared() {
        privateSync.stopSync()
        super.onCleared()
    }

    sealed class State {
        object Starting : State()
        sealed class Syncing : State() {
            object WaitingFirstClient : Syncing()
            data class HadFirstClient(val clientsConnected: Int) : Syncing()
        }

        object Error : State()
    }
}
