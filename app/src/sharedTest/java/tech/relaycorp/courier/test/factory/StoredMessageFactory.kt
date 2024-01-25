package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.model.GatewayType
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.relaynet.messages.Recipient
import java.util.Date
import java.util.Random

object StoredMessageFactory {
    fun build(recipient: Recipient = Recipient("0deadbeef")): StoredMessage {
        val recipientAddress = recipient.internetAddress ?: recipient.id
        val recipientType =
            if (recipient.internetAddress != null) GatewayType.Internet else GatewayType.Private
        return StoredMessage(
            recipientAddress,
            recipientType,
            senderId = Random().nextInt().toString(),
            messageId = MessageId(Random().nextInt().toString()),
            messageType = MessageType.values()[Random().nextInt(MessageType.values().size)],
            creationTimeUtc = Date(),
            expirationTimeUtc = Date(),
            storagePath = "",
            size = StorageSizeFactory.build(),
        )
    }
}
