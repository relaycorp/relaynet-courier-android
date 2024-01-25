package tech.relaycorp.courier.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import tech.relaycorp.cogrpc.server.CogRPCServer
import tech.relaycorp.courier.domain.server.ServerService
import javax.inject.Inject

class PrivateSync
    @Inject
    constructor(
        private val cogRPCServer: CogRPCServer,
        private val service: ServerService,
    ) {
        private val state = MutableStateFlow<State?>(null)

        fun state() = state.asStateFlow().filterNotNull()

        fun clientsConnected() = cogRPCServer.clientsConnected()

        suspend fun startSync() {
            if (cogRPCServer.isStarted) throw IllegalStateException("Sync already started")

            state.value = State.Starting
            cogRPCServer.start(service) {
                state.value = State.Error
            }
            if (state.value == State.Starting) state.value = State.Syncing
        }

        fun stopSync() {
            cogRPCServer.stop()
            state.value = State.Stopped
        }

        enum class State {
            Starting,
            Syncing,
            Stopped,
            Error,
        }
    }
