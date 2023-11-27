package tech.relaycorp.courier.data.database

import androidx.room.TypeConverter
import tech.relaycorp.courier.data.model.GatewayType
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType

class MessageConverter {
    @TypeConverter
    fun toAddressType(value: String) = GatewayType.fromValue(value)

    @TypeConverter
    fun fromAddressType(gatewayType: GatewayType) = gatewayType.value

    @TypeConverter
    fun toId(value: String) = MessageId(value)

    @TypeConverter
    fun fromId(id: MessageId) = id.value

    @TypeConverter
    fun toType(value: String) = MessageType.fromValue(value)

    @TypeConverter
    fun fromType(type: MessageType) = type.value
}
