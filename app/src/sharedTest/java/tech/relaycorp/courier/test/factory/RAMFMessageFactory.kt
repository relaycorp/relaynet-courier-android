package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.network.Cargo

object RAMFMessageFactory {
    fun buildCargo(size: Int = 0) = Cargo.deserialize(ByteArray(size))
}
