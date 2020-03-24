package tech.relaycorp.courier.data.network.cogrpc

import java.io.InputStream

object CogRPC {
    data class MessageDelivery(
        val localId: String,
        val data: InputStream
    )

    data class MessageDeliveryAck(
        val localId: String
    )

    data class MessageReceived(
        val data: InputStream
    )
}
