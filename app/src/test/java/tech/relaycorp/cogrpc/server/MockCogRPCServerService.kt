package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
import tech.relaycorp.relaynet.CargoDeliveryRequest
import java.io.InputStream

open class MockCogRPCServerService : CogRPCServer.Service {
    var collectCargoCalls = mutableListOf<ByteArray>()
    var processCargoCollectionAckCalls = mutableListOf<String>()
    var deliverCargoCalls = mutableListOf<InputStream>()

    override suspend fun collectCargo(ccaSerialized: ByteArray): Iterable<CargoDeliveryRequest> {
        collectCargoCalls.add(ccaSerialized)
        return listOf(
            CargoDeliveryRequest(
                "1234",
                CARGO_DELIVERED::newInput,
            ),
        )
    }

    override suspend fun processCargoCollectionAck(localId: String) {
        processCargoCollectionAckCalls.add(localId)
    }

    override suspend fun deliverCargo(cargoSerialized: InputStream): CogRPCServer.DeliverResult {
        deliverCargoCalls.add(cargoSerialized)
        return CogRPCServer.DeliverResult.Successful
    }

    companion object {
        val CARGO_DELIVERED: ByteString = ByteString.copyFromUtf8("ABC")
    }
}
