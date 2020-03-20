package tech.relaycorp.courier.data.network

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

// Mock implementation of the CogRPC protocol that just waits a bit and emits Acks
class MockCogRPC : CogRPC {

    override fun deliverCargo(
        address: String,
        cargoes: List<CogRPC.MessageDelivery>
    ) =
        cargoes
            .asFlow()
            .onEach { delay(500) }
            .map { cargo ->
                CogRPC.MessageDeliveryAck(
                    cargo.senderAddress,
                    cargo.messageId
                )
            }

    override fun collectCargo(
        address: String,
        cca: CogRPC.MessageDelivery
    ): Flow<CogRPC.MessageReceived> =
        flow {
            delay(500)
            emit(CogRPC.MessageReceived(ByteArray(0).inputStream()))
        }
}
