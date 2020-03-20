package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import java.util.Date
import java.util.Random

object StoredMessageFactory {
    fun build() = StoredMessage(
        recipientAddress = MessageAddress(Random().nextInt().toString()),
        recipientType = MessageAddress.Type.values()[Random().nextInt(MessageAddress.Type.values().size)],
        senderAddress = MessageAddress(Random().nextInt().toString()),
        messageId = MessageId(Random().nextInt().toString()),
        messageType = MessageType.values()[Random().nextInt(MessageType.values().size)],
        creationTimeUtc = Date(),
        expirationTimeUtc = Date(),
        storagePath = "",
        size = Random().nextInt(1000).toLong()
    )
}
