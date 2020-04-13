package tech.relaycorp.relaynet

import kotlinx.coroutines.flow.Flow

interface CargoRelayServer {

    val isStarted: Boolean

    suspend fun start(
        connectionService: ConnectionService,
        onForcedStop: ((Throwable) -> Unit)
    )

    suspend fun stop()

    fun clientsConnected(): Flow<Int>

    interface ConnectionService {
        suspend fun collectCargo(cca: CargoRelay.MessageReceived): Iterable<CargoRelay.MessageDelivery>
        suspend fun processCargoCollectionAck(ack: CargoRelay.MessageDeliveryAck)
        suspend fun deliverCargo(cargo: CargoRelay.MessageReceived)
    }

    interface Builder {
        fun build(networkLocation: String): CargoRelayServer
    }
}
