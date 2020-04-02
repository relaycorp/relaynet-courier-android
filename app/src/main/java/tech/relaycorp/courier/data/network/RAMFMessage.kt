package tech.relaycorp.courier.data.network

import java.io.InputStream
import java.util.Date
import java.util.UUID

// Mock RAMFMessage to help us with the high-level implementation
// The fields won't be available exactly like this, but it's enough for now
open class RAMFMessage protected constructor(
    val recipientAddress: String = UUID.randomUUID().toString(),
    val senderPrivateAddress: String = UUID.randomUUID().toString(),
    val messageId: String = UUID.randomUUID().toString(),
    val creationTime: Date = Date(),
    val ttl: Int = Int.MAX_VALUE,
    val payload: ByteArray = ByteArray(0)
)

class Cargo private constructor(payload: ByteArray) : RAMFMessage(payload = payload) {
    companion object {
        fun wrap(data: InputStream) = Cargo(data.readBytes())
    }
}

class CargoCollectionAuthorization private constructor() : RAMFMessage() {
    companion object {
        fun wrap(data: InputStream) = CargoCollectionAuthorization()
    }
}
