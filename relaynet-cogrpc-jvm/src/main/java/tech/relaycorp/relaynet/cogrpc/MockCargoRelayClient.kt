package tech.relaycorp.relaynet.cogrpc

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.relaynet.CargoRelay
import tech.relaycorp.relaynet.CargoRelayClient

// Mock implementation of the CogRPC protocol that just waits a bit and emits Acks
class MockCargoRelayClient
internal constructor(
    serverAddress: String
) : CargoRelayClient {

    override fun deliverCargo(cargoes: Iterable<CargoRelay.MessageDelivery>) =
        cargoes
            .asFlow()
            .onEach { delay(500) }
            .map { cargo ->
                CargoRelay.MessageDeliveryAck(cargo.localId)
            }

    override fun collectCargo(cca: CargoRelay.MessageDelivery): Flow<CargoRelay.MessageReceived> =
        flow {
            delay(500)
            emit(
                CargoRelay.MessageReceived("CARGO".toByteArray().inputStream())
            )
        }

    object Builder : CargoRelayClient.Builder {
        override fun build(serverAddress: String) =
            MockCargoRelayClient(serverAddress)
    }
}
