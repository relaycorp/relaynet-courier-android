package tech.relaycorp.courier.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Date

@Entity(
    tableName = "Message",
    primaryKeys = ["senderAddress", "messageId"]
)
data class StoredMessage(
    @ColumnInfo(index = true)
    val recipientAddress: MessageAddress,
    @ColumnInfo(index = true)
    val recipientType: MessageAddress.Type,
    val senderAddress: MessageAddress,
    val messageId: MessageId,
    @ColumnInfo(index = true)
    val messageType: MessageType,
    val creationTimeUtc: Date, // in UTC
    @ColumnInfo(index = true)
    val expirationTimeUtc: Date, // in UTC
    val storagePath: String,
    val size: Long
)
