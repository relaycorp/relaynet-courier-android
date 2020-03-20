package tech.relaycorp.courier.data.network

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface CogRPC {

    fun deliverCargo(address: String, cargoes: List<MessageDelivery>): Flow<MessageDeliveryAck>

    fun collectCargo(address: String, cca: MessageDelivery): Flow<MessageReceived>

    data class MessageDelivery(
        val senderAddress: String,
        val messageId: String,
        val data: InputStream
    )

    data class MessageDeliveryAck(
        val senderAddress: String,
        val messageId: String
    )

    data class MessageReceived(
        val data: InputStream
    )
}
