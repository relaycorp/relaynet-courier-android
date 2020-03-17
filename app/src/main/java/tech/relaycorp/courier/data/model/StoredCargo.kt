package tech.relaycorp.courier.data.model

import androidx.room.Entity
import androidx.room.Index
import java.util.Date

@Entity(
    tableName = "cargo",
    primaryKeys = ["recipientAddress", "messageId"],
    indices = [
        Index(value = ["expirationTime"]),
        Index(value = ["senderAddress"])
    ]
)
data class StoredCargo(
    val recipientAddress: MessageAddress,
    val messageId: MessageId,
    val creationTime: Date, // in UTC
    val expirationTime: Date, // in UTC
    val ttl: Int, // seconds
    val storagePath: String,
    val size: Long,
    val senderAddress: MessageAddress
)
