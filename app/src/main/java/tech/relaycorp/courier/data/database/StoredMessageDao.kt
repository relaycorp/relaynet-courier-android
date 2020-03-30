package tech.relaycorp.courier.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PrivateMessageAddress
import tech.relaycorp.courier.data.model.StoredMessage

@Dao
interface StoredMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: StoredMessage)

    @Delete
    suspend fun delete(message: StoredMessage)

    @Query("SELECT * FROM Message")
    fun observeAll(): Flow<List<StoredMessage>>

    @Query("SELECT COALESCE(SUM(Message.size), 0) FROM Message")
    fun observeFullSize(): Flow<Long>

    @Query("SELECT * FROM Message WHERE senderAddress = :senderAddress AND messageId = :messageId LIMIT 1")
    suspend fun get(senderAddress: PrivateMessageAddress, messageId: MessageId): StoredMessage

    @Query(
        """
            SELECT * FROM Message 
            WHERE recipientType = :recipientType AND messageType = :type
            ORDER BY expirationTimeUtc ASC
            """
    )
    suspend fun getByRecipientTypeAndMessageType(
        recipientType: MessageAddress.Type,
        type: MessageType
    ): List<StoredMessage>

    @Query(
        """
            SELECT * FROM Message 
            WHERE recipientAddress = :recipientAddress AND messageType = :type
            ORDER BY expirationTimeUtc ASC
            """
    )
    suspend fun getByRecipientAddressAndMessageType(
        recipientAddress: MessageAddress,
        type: MessageType
    ): List<StoredMessage>
}
