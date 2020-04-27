package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
import tech.relaycorp.relaynet.cogrpc.CogRPC

open class MockCogRPCServerService : CogRPCServer.Service {

    var collectCargoCalls = mutableListOf<CogRPC.MessageDelivery>()
    var processCargoCollectionAckCalls = mutableListOf<CogRPC.MessageDeliveryAck>()
    var deliverCargoCalls = mutableListOf<CogRPC.MessageDelivery>()

    override suspend fun collectCargo(cca: CogRPC.MessageDelivery): Iterable<CogRPC.MessageDelivery> {
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

    override suspend fun deliverCargo(cargo: CogRPC.MessageDelivery): Boolean {
        deliverCargoCalls.add(cargo)
        return true
    }

    companion object {
        val CARGO_DELIVERED: ByteString = ByteString.copyFromUtf8("ABC")
    }
}
