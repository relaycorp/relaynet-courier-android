package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.data.network.Message
import java.io.InputStream
import java.util.Date
import javax.inject.Inject

class StoreMessage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository
) {

    suspend fun storeCargo(
        senderAddress: MessageAddress,
        recipientType: MessageAddress.Type,
        data: InputStream
    ) {
        val message = Message.wrap(data)
        val storagePath = diskRepository.writeMessage(message.payload)
        val storedMessage = StoredMessage(
            recipientAddress = MessageAddress(message.recipientAddress),
            recipientType = recipientType, // Or can we tell from the recipientAddress?
            senderAddress = senderAddress,
            messageId = MessageId(message.messageId),
            messageType = MessageType.Cargo,
            creationTimeUtc = message.creationTime,
            expirationTimeUtc = Date(message.creationTime.time + message.ttl * 1000),
            size = message.payload.size.toLong(),
            storagePath = storagePath
        )
        storedMessageDao.insert(storedMessage)
    }
}
