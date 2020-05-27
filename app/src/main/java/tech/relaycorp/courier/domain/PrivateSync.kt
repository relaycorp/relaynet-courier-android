package tech.relaycorp.courier.domain

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import tech.relaycorp.cogrpc.server.CogRPCServer
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.domain.server.ServerService
import javax.inject.Inject

class PrivateSync
@Inject constructor(
    private val cogRPCServer: CogRPCServer,
    private val service: ServerService
) {

    private val state = BehaviorChannel<State>()
    fun state() = state.asFlow()

    fun clientsConnected() = cogRPCServer.clientsConnected()

    suspend fun startSync() {
        if (cogRPCServer.isStarted) throw IllegalStateException("Sync already started")

        state.send(State.Starting)
        cogRPCServer.start(service) {
            state.sendBlocking(State.Error)
        }
        if (state.value == State.Starting) state.send(State.Syncing)
    }

    fun stopSync() {
        cogRPCServer.stop()
        state.sendBlocking(State.Stopped)
    }

    enum class State {
        Starting, Syncing, Stopped, Error
    }
}
