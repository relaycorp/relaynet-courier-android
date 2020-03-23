package tech.relaycorp.courier.data.network

import kotlinx.coroutines.flow.Flow
import java.io.InputStream

abstract class CogRPCClient
protected constructor(
    val serverAddress: String
) {

    @Throws(Exception::class)
    abstract fun deliverCargo(cargoes: List<MessageDelivery>): Flow<MessageDeliveryAck>

    @Throws(Exception::class)
    abstract fun collectCargo(cca: MessageDelivery): Flow<MessageReceived>

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

    class Exception : Error()

    companion object {
        fun build(serverAddress: String) = MockCogRPCClient(serverAddress)
    }
}
