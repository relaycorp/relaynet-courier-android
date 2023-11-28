package tech.relaycorp.courier.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.courier.data.model.GatewayType
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StoredMessage
import java.util.Date

@Dao
interface StoredMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: StoredMessage)

    @Delete
    suspend fun delete(message: StoredMessage)

    @Query("DELETE FROM Message")
    suspend fun deleteAll()

    @Query("SELECT * FROM Message")
    fun observeAll(): Flow<List<StoredMessage>>

    @Query("SELECT COALESCE(SUM(Message.size), 0) FROM Message")
    fun observeTotalSize(): Flow<StorageSize>

    @Query("SELECT * FROM Message WHERE senderId = :senderId AND messageId = :messageId LIMIT 1")
    suspend fun get(
        senderId: String,
        messageId: MessageId,
    ): StoredMessage

    @Query(
        """
            SELECT * FROM Message 
            WHERE recipientType = :recipientType AND messageType = :type
            ORDER BY expirationTimeUtc ASC
            """,
    )
    suspend fun getByRecipientTypeAndMessageType(
        recipientType: GatewayType,
        type: MessageType,
    ): List<StoredMessage>

    @Query(
        """
            SELECT * FROM Message 
            WHERE recipientAddress = :recipientAddress AND messageType = :type
            ORDER BY expirationTimeUtc ASC
            """,
    )
    suspend fun getByRecipientAddressAndMessageType(
        recipientAddress: String,
        type: MessageType,
    ): List<StoredMessage>

    @Query("SELECT * FROM Message WHERE expirationTimeUtc <= :timeUtc")
    suspend fun getExpiredBy(timeUtc: Date): List<StoredMessage>

    @Query("SELECT COUNT(messageId) FROM Message WHERE messageType <= :type")
    fun observeCountByMessageType(type: MessageType): Flow<Long>
}
