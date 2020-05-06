package tech.relaycorp.courier.test.factory

import tech.relaycorp.relaynet.Cargo
import tech.relaycorp.relaynet.CargoCollectionAuthorization

object RAMFMessageFactory {
    fun buildCargo(size: Int = 0) = Cargo.deserialize(ByteArray(size))
    fun buildCCA(size: Int = 0) = CargoCollectionAuthorization.deserialize(ByteArray(size))
}