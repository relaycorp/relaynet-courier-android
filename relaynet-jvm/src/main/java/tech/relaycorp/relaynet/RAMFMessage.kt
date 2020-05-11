package tech.relaycorp.relaynet

import java.util.Date
import java.util.UUID

// Mock RAMFMessage to help us with the high-level implementation
// The fields won't be available exactly like this, but it's enough for now
open class RAMFMessage protected constructor(
    val recipientAddress: String = UUID.randomUUID().toString(),
    val senderPrivateAddress: String = UUID.randomUUID().toString(),
    val messageId: String = UUID.randomUUID().toString(),
    val creationDate: Date = Date(),
    val ttl: Int = 60 * 60 * 24 * 10
) {
    fun isValid(): Boolean = true
}

class Cargo private constructor() : RAMFMessage() {
    companion object {
        @Throws(RAMFMessageMalformedException::class)
        fun deserialize(data: ByteArray) = Cargo()
    }
}

class RAMFMessageMalformedException(message: String? = null) : Exception(message)
