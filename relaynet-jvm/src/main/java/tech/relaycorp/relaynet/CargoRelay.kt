package tech.relaycorp.relaynet

import java.io.InputStream

object CargoRelay {
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
