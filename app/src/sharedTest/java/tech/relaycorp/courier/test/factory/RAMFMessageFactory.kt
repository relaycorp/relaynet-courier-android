package tech.relaycorp.courier.test.factory

import tech.relaycorp.relaynet.Cargo

object RAMFMessageFactory {
    fun buildCargo(size: Int = 0) = Cargo.deserialize(ByteArray(size))
}
