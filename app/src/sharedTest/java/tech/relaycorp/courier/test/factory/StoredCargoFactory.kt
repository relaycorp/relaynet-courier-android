package tech.relaycorp.courier.test.factory

import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.StoredCargo
import java.util.Date
import java.util.Random

object StoredCargoFactory {
    fun build() = StoredCargo(
        recipientAddress = MessageAddress(Random().nextInt().toString()),
        messageId = MessageId(Random().nextInt().toString()),
        creationTime = Date(),
        expirationTime = Date(),
        storagePath = "",
        size = Random().nextInt(1000).toLong(),
        senderAddress = MessageAddress(Random().nextInt().toString())
    )
}
