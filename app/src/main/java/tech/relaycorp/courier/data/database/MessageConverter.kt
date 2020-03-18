package tech.relaycorp.courier.data.database

import androidx.room.TypeConverter
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId

class MessageConverter {
    @TypeConverter
    fun toAddress(value: String) = MessageAddress(value)

    @TypeConverter
    fun fromAddress(address: MessageAddress) = address.value

    @TypeConverter
    fun toId(value: String) = MessageId(value)

    @TypeConverter
    fun fromId(id: MessageId) = id.value
}
