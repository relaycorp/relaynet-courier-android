package tech.relaycorp.courier.data.network

import java.io.InputStream
import java.util.Date

// Mock RAMFMessage to help us with the high-level implementation
// The fields won't be available exactly like this, but it's enough for now
class Message(
    val recipientPublicAddress: String?,
    val recipientPrivateAddress: String,
    val senderPrivateAddress: String,
    val messageId: String,
    val creationTime: Date,
    val ttl: Int,
    val payload: ByteArray
) {
    companion object {
        fun wrap(data: InputStream) = Message("", "", "", "", Date(), 0, ByteArray(0))
    }
}
