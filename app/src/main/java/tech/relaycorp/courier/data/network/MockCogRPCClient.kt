package tech.relaycorp.courier.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// Mock implementation of the CogRPC protocol that just waits a bit and emits Acks
class MockCogRPCClient(serverAddress: String) : CogRPCClient(serverAddress) {

    override fun deliverCargo(cargoes: List<MessageDelivery>) =
        cargoes
            .asFlow()
            .onEach { delay(500) }
            .map { cargo ->
                MessageDeliveryAck(cargo.localId)
            }

    override fun collectCargo(cca: MessageDelivery): Flow<MessageReceived> =
        flow {
            delay(500)
            emit(MessageReceived(ByteArray(0).inputStream()))
        }
}
