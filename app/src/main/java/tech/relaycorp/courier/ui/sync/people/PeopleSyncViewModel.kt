package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateWatcher
import tech.relaycorp.courier.common.PublishFlow
import tech.relaycorp.courier.domain.PrivateSync
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import tech.relaycorp.courier.ui.common.Finish
import javax.inject.Inject

class PeopleSyncViewModel
    @Inject
    constructor(
        private val privateSync: PrivateSync,
        wifiHotspotStateWatcher: WifiHotspotStateWatcher,
    ) : BaseViewModel() {
        // Inputs

        fun stopClicked() = stopClicks.tryEmit(Click)

        private val stopClicks = PublishFlow<Click>()

        fun confirmStopClicked() = confirmStopClicks.tryEmit(Click)

        private val confirmStopClicks = PublishFlow<Click>()

        // Outputs

        private val state = MutableStateFlow<State?>(null)

        fun state() = state.asStateFlow().filterNotNull()

        private val openHotspotInstructions = MutableStateFlow<Unit?>(null)

        fun openHotspotInstructions() = openHotspotInstructions.asStateFlow().filterNotNull()

        private val confirmStop = PublishFlow<Unit>()

        fun confirmStop() = confirmStop.asSharedFlow()

        private val finish = MutableStateFlow<Finish?>(null)

        fun finish() = finish.asStateFlow().filterNotNull()

        private var hadFirstClient = false

        init {
            wifiHotspotStateWatcher
                .state()
                .take(1)
                .onEach {
                    when (it) {
                        WifiHotspotState.Enabled -> privateSync.startSync()
                        WifiHotspotState.Disabled -> {
                            openHotspotInstructions.value = Unit
                            finish.value = Finish
                        }
                    }
                }
                .launchIn(scope)

            wifiHotspotStateWatcher
                .state()
                .drop(1)
                .filter { it == WifiHotspotState.Disabled }
                .onEach {
                    privateSync.stopSync()
                    openHotspotInstructions.value = Unit
                    finish.value = Finish
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
                privateSync.clientsConnected(),
            ) { syncState, clientsConnected ->
                when (syncState) {
                    PrivateSync.State.Starting -> state.value = State.Starting
                    PrivateSync.State.Syncing ->
                        state.value =
                            if (hadFirstClient) {
                                State.Syncing.HadFirstClient(clientsConnected)
                            } else {
                                State.Syncing.WaitingFirstClient
                            }
                    PrivateSync.State.Stopped -> finish.value = Finish
                    PrivateSync.State.Error -> state.value = State.Error
                }
            }
                .launchIn(scope)

            stopClicks
                .onEach {
                    if (state.value == State.Starting) return@onEach

                    val clientsConnected =
                        (state.value as? State.Syncing.HadFirstClient)?.clientsConnected ?: 0
                    if (clientsConnected > 0) {
                        confirmStop.emit(Unit)
                    } else {
                        finish.value = Finish
                    }
                }
                .launchIn(scope)

            confirmStopClicks
                .onEach { finish.value = Finish }
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
