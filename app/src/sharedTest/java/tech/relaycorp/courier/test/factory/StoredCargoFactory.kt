package tech.relaycorp.courier.test.factory

import java.util.Date
import java.util.Random
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.StoredCargo

object StoredCargoFactory {
    fun build() = StoredCargo(
        recipientAddress = MessageAddress(Random().nextInt().toString()),
        messageId = MessageId(Random().nextInt().toString()),
        creationTime = Date(),
        expirationTime = Date(),
        ttl = Random().nextInt(1000),
        storagePath = "",
        size = Random().nextInt(1000).toLong(),
        senderAddress = MessageAddress(Random().nextInt().toString())
    )
}
