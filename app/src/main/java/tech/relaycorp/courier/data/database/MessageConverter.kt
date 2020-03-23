package tech.relaycorp.courier.data.database

import androidx.room.TypeConverter
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PrivateMessageAddress

class MessageConverter {
    @TypeConverter
    fun toAddress(value: String) = MessageAddress.of(value)

    @TypeConverter
    fun fromAddress(address: MessageAddress) = address.value

    @TypeConverter
    fun toPrivateAddress(value: String) = PrivateMessageAddress(value)

    @TypeConverter
    fun fromPrivateAddress(address: PrivateMessageAddress) = address.value

    @TypeConverter
    fun toAddressType(value: String) = MessageAddress.Type.fromValue(value)

    @TypeConverter
    fun fromAddressType(type: MessageAddress.Type) = type.value

    @TypeConverter
    fun toId(value: String) = MessageId(value)

    @TypeConverter
    fun fromId(id: MessageId) = id.value

    @TypeConverter
    fun toType(value: String) = MessageType.fromValue(value)

    @TypeConverter
    fun fromType(type: MessageType) = type.value
}
