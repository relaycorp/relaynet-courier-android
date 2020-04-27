package tech.relaycorp.cogrpc.server

import tech.relaycorp.relaynet.cogrpc.CogRPC

object CogRPCFactory {
    fun buildDelivery(id: String = "1234", cargo: String = "ABC") =
        CogRPC.MessageDelivery(id, cargo.byteInputStream()).toCargoDelivery()

    fun buildDeliveryAck(id: String) =
        CogRPC.MessageDeliveryAck(id).toCargoDeliveryAck()
}
