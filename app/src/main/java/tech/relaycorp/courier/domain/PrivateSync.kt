package tech.relaycorp.courier.domain

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.relaynet.CargoRelayServer
import tech.relaycorp.courier.domain.server.ServerConnectionService
import javax.inject.Inject

class PrivateSync
@Inject constructor(
    private val cargoRelayServer: CargoRelayServer,
    private val connectionService: ServerConnectionService
) {

    private val state = BehaviorChannel<State>()
    fun state() = state.asFlow()

    fun clientsConnected() = cargoRelayServer.clientsConnected()

    suspend fun startSync() {
        if (cargoRelayServer.isStarted) throw IllegalStateException("Sync already started")

        cargoRelayServer.start(connectionService) {
            state.sendBlocking(State.Stopped)
        }
        state.send(State.Syncing)
    }

    suspend fun stopSync() {
        cargoRelayServer.stop()
        state.send(State.Stopped)
    }

    enum class State {
        Stopped, Syncing
    }
}
