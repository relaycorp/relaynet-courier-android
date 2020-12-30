package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PrivateMessageAddress
import tech.relaycorp.courier.data.model.StoredMessage
import java.util.Date
import java.util.Random

object StoredMessageFactory {
    fun build(
        recipientAddress: MessageAddress = MessageAddress.of(Random().nextInt().toString())
    ): StoredMessage {
        return StoredMessage(
            recipientAddress = recipientAddress,
            recipientType = recipientAddress.type,
            senderAddress = PrivateMessageAddress(Random().nextInt().toString()),
            messageId = MessageId(Random().nextInt().toString()),
            messageType = MessageType.values()[Random().nextInt(MessageType.values().size)],
            creationTimeUtc = Date(),
            expirationTimeUtc = Date(),
            storagePath = "",
            size = StorageSizeFactory.build()
        )
    }
}
