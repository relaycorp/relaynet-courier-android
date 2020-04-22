package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
import tech.relaycorp.relaynet.cogrpc.CogRPC

class TestCogRPCServerService : CogRPCServer.Service {

    var collectCargoCalls = mutableListOf<CogRPC.MessageReceived>()
    var processCargoCollectionAckCalls = mutableListOf<CogRPC.MessageDeliveryAck>()
    var deliverCargoCalls = mutableListOf<CogRPC.MessageReceived>()

    override suspend fun collectCargo(cca: CogRPC.MessageReceived): Iterable<CogRPC.MessageDelivery> {
        collectCargoCalls.add(cca)
        return listOf(
            CogRPC.MessageDelivery(
                "1234",
                CARGO_DELIVERED.newInput()
            )
        )
    }

    override suspend fun processCargoCollectionAck(ack: CogRPC.MessageDeliveryAck) {
        processCargoCollectionAckCalls.add(ack)
    }

    override suspend fun deliverCargo(cargo: CogRPC.MessageReceived) {
        deliverCargoCalls.add(cargo)
    }

    companion object {
        val CARGO_DELIVERED: ByteString = ByteString.copyFromUtf8("ABC")
    }
}
