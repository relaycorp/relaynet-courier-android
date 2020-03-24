package tech.relaycorp.courier.data.network.cogrpc

import kotlinx.coroutines.flow.Flow

abstract class CogRPCClient
protected constructor(
    val serverAddress: String
) {

    @Throws(Exception::class)
    abstract fun deliverCargo(cargoes: Iterable<CogRPC.MessageDelivery>): Flow<CogRPC.MessageDeliveryAck>

    @Throws(Exception::class)
    abstract fun collectCargo(cca: CogRPC.MessageDelivery): Flow<CogRPC.MessageReceived>

    class Exception : Error()

    companion object {
        fun build(serverAddress: String) =
            MockCogRPCClient(serverAddress)
    }
}
