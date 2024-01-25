package tech.relaycorp.courier.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.util.Date
import java.util.UUID

@Entity(
    tableName = "Message",
    primaryKeys = ["senderId", "messageId"],
)
data class StoredMessage(
    @ColumnInfo(index = true)
    val recipientAddress: String,
    @ColumnInfo(index = true)
    val recipientType: GatewayType,
    val senderId: String,
    val messageId: MessageId,
    @ColumnInfo(index = true)
    val messageType: MessageType,
    val creationTimeUtc: Date,
    @ColumnInfo(index = true)
    val expirationTimeUtc: Date,
    val storagePath: String,
    val size: StorageSize,
) {
    companion object {
        fun generateLocalId() = UUID.randomUUID().toString()
    }
}
