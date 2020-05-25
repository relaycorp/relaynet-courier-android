package tech.relaycorp.relaynet.cogrpc

import com.google.protobuf.ByteString
import tech.relaycorp.relaynet.CargoDeliveryRequest

fun CargoDeliveryRequest.toCargoDelivery() =
    CargoDelivery.newBuilder()
        .setId(localId)
        .setCargo(ByteString.copyFrom(cargoSerialized().readBytesAndClose()))
        .build()

fun CargoDelivery.toAck() = id.toCargoDeliveryAck()

fun String.toCargoDeliveryAck() =
    CargoDeliveryAck.newBuilder()
        .setId(this)
        .build()
