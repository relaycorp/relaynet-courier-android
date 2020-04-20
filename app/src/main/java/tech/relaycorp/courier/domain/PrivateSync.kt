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

        cogRPCServer.start(service) {
            state.sendBlocking(State.Stopped)
        }
        state.send(State.Syncing)
    }

    suspend fun stopSync() {
        cogRPCServer.stop()
        state.send(State.Stopped)
    }

    enum class State {
        Stopped, Syncing
    }
}
