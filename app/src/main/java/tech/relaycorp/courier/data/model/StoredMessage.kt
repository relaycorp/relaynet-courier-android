package tech.relaycorp.courier.data.model

import androidx.room.Entity
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "Message",
    primaryKeys = ["senderAddress", "messageId"],
    indices = [
        Index(value = ["recipientAddress"]),
        Index(value = ["messageType"]),
        Index(value = ["expirationTime"])
    ]
)
data class StoredMessage(
    val recipientAddress: MessageAddress,
    val recipientType: MessageAddress.Type,
    val senderAddress: MessageAddress,
    val messageId: MessageId,
    val messageType: MessageType,
    val creationTime: Date, // in UTC
    val expirationTime: Date, // in UTC
    val storagePath: String,
    val size: Long
)
