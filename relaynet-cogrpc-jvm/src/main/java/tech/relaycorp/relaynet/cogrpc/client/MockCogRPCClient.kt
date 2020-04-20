package tech.relaycorp.relaynet.cogrpc.client

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.relaynet.cogrpc.CogRPC

// Mock implementation of the CogRPC protocol that just waits a bit and emits Acks
class MockCogRPCClient
internal constructor() : CogRPCClient {

    override fun deliverCargo(cargoes: Iterable<CogRPC.MessageDelivery>) =
        cargoes
            .asFlow()
            .onEach { delay(500) }
            .map { cargo ->
                CogRPC.MessageDeliveryAck(cargo.localId)
            }

    override fun collectCargo(cca: CogRPC.MessageDelivery): Flow<CogRPC.MessageReceived> =
        flow {
            delay(500)
            emit(
                CogRPC.MessageReceived(
                    "CARGO".toByteArray().inputStream()
                )
            )
        }

    object Builder : CogRPCClient.Builder {
        override fun build(serverAddress: String) =
            MockCogRPCClient()
    }
}
