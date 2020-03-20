package tech.relaycorp.courier.data.network

import java.io.InputStream
import java.util.Date

// Mock RAMFMessage to help us with the high-level implementation
class Message(
    val recipientAddress: String,
    val messageId: String,
    val creationTime: Date,
    val ttl: Int,
    val payload: ByteArray
) {
    companion object {
        fun wrap(data: InputStream) = Message("", "", Date(), 0, ByteArray(0))
    }
}
