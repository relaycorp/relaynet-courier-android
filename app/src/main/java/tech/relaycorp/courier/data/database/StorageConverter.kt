package tech.relaycorp.courier.data.database

import androidx.room.TypeConverter
import tech.relaycorp.courier.data.model.StorageSize

class StorageConverter {
    @TypeConverter
    fun toStorageSize(value: Long) = StorageSize(value)

    @TypeConverter
    fun fromStorageSize(size: StorageSize) = size.bytes
}
