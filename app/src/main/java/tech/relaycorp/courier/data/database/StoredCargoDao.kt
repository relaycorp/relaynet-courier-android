package tech.relaycorp.courier.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.StoredCargo

@Dao
interface StoredCargoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cargo: StoredCargo)

    @Delete
    suspend fun delete(cargo: StoredCargo)

    @Query("SELECT * FROM cargo")
    fun getAll(): Flow<List<StoredCargo>>

    @Query("SELECT * FROM cargo WHERE recipientAddress = :recipientAddress")
    fun getForRecipient(recipientAddress: MessageAddress): Flow<List<StoredCargo>>

    @Query("SELECT SUM(cargo.size) FROM cargo")
    fun getFullSize(): Flow<Long>
}
