package tech.relaycorp.courier.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import tech.relaycorp.courier.data.model.StoredMessage

@Database(
    entities = [StoredMessage::class],
    version = 1
)
@TypeConverters(
    value = [
        DateConverter::class,
        MessageConverter::class,
        StorageConverter::class
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun storedMessageDao(): StoredMessageDao
}
