package tech.relaycorp.relaynet.cogrpc.client

import kotlinx.coroutines.flow.Flow
import tech.relaycorp.relaynet.cogrpc.CogRPC

interface CogRPCClient {

    @Throws(Exception::class)
    fun deliverCargo(cargoes: Iterable<CogRPC.MessageDelivery>): Flow<CogRPC.MessageDeliveryAck>

    @Throws(Exception::class)
    fun collectCargo(cca: CogRPC.MessageDelivery): Flow<CogRPC.MessageReceived>

    class Exception : Error()

    interface Builder {
        fun build(serverAddress: String): CogRPCClient
    }
}
