package tech.relaycorp.courier.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.StoredMessage

@Dao
interface StoredMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: StoredMessage)

    @Delete
    suspend fun delete(message: StoredMessage)

    @Query("SELECT * FROM Message")
    fun getAll(): Flow<List<StoredMessage>>

    @Query("SELECT * FROM Message WHERE recipientAddress = :recipientAddress")
    fun getForRecipient(recipientAddress: MessageAddress): Flow<List<StoredMessage>>

    @Query("SELECT SUM(Message.size) FROM Message")
    fun getFullSize(): Flow<Long>
}
