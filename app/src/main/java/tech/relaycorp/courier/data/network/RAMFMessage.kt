package tech.relaycorp.courier.data.network

import java.io.InputStream
import java.util.Date

// Mock RAMFMessage to help us with the high-level implementation
// The fields won't be available exactly like this, but it's enough for now
open class RAMFMessage protected constructor(
    val recipientPublicAddress: String? = "",
    val recipientPrivateAddress: String = "",
    val senderPrivateAddress: String = "",
    val messageId: String = "",
    val creationTime: Date = Date(),
    val ttl: Int = 0,
    val payload: ByteArray = ByteArray(0)
)

class Cargo private constructor() : RAMFMessage() {
    companion object {
        fun wrap(data: InputStream) = Cargo()
    }
}

class CargoCollectionAuthorization private constructor() : RAMFMessage() {
    companion object {
        fun wrap(data: InputStream) = CargoCollectionAuthorization()
    }
}
