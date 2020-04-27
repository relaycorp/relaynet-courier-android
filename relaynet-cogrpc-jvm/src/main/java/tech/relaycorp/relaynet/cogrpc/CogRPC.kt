package tech.relaycorp.relaynet.cogrpc

import com.google.protobuf.ByteString
import java.io.InputStream

object CogRPC {
    data class MessageDelivery(
        val localId: String = "",
        val data: InputStream
    ) {
        constructor(cargoDelivery: CargoDelivery) : this(
            cargoDelivery.id,
            cargoDelivery.cargo.toByteArray().inputStream()
        )

        fun toCargoDelivery() =
            CargoDelivery.newBuilder()
                .setId(localId)
                .setCargo(ByteString.copyFrom(data.readBytes()))
                .build()

        fun toCargoDeliveryAck() =
            CargoDeliveryAck.newBuilder()
                .setId(localId)
                .build()
    }

    data class MessageDeliveryAck(
        val localId: String
    ) {
        constructor(cargoDeliveryAck: CargoDeliveryAck) : this(cargoDeliveryAck.id)

        fun toCargoDeliveryAck() =
            CargoDeliveryAck.newBuilder()
                .setId(localId)
                .build()
    }
}
