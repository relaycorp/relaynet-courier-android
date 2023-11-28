package tech.relaycorp.cogrpc.server

import com.google.protobuf.ByteString
import tech.relaycorp.relaynet.cogrpc.CargoDelivery
import tech.relaycorp.relaynet.cogrpc.CargoDeliveryAck

object DataFactory {
    fun buildDelivery(
        id: String = "1234",
        cargo: String = "ABC",
    ) = CargoDelivery.newBuilder().setId(id).setCargo(ByteString.copyFromUtf8(cargo)).build()

    fun buildDeliveryAck(id: String = "1234") = CargoDeliveryAck.newBuilder().setId(id).build()
}
