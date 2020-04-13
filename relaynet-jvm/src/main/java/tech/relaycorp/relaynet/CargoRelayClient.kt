package tech.relaycorp.relaynet

import kotlinx.coroutines.flow.Flow

interface CargoRelayClient {

    @Throws(Exception::class)
    fun deliverCargo(cargoes: Iterable<CargoRelay.MessageDelivery>): Flow<CargoRelay.MessageDeliveryAck>

    @Throws(Exception::class)
    fun collectCargo(cca: CargoRelay.MessageDelivery): Flow<CargoRelay.MessageReceived>

    class Exception : Error()

    interface Builder {
        fun build(serverAddress: String): CargoRelayClient
    }
}
