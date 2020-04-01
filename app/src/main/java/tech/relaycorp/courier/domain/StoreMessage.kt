package tech.relaycorp.courier.domain

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PrivateMessageAddress
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.data.network.Cargo
import tech.relaycorp.courier.data.network.CargoCollectionAuthorization
import tech.relaycorp.courier.data.network.RAMFMessage
import java.io.InputStream
import java.util.Date
import javax.inject.Inject

class StoreMessage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository
) {

    suspend fun storeCargo(data: InputStream) =
        storeMessage(Cargo.wrap(data), MessageType.Cargo)

    suspend fun storeCCA(data: InputStream) =
        storeMessage(CargoCollectionAuthorization.wrap(data), MessageType.Cargo)

    private suspend fun storeMessage(message: RAMFMessage, type: MessageType): StoredMessage {
        val storagePath = diskRepository.writeMessage(message.payload)
        val storedMessage = message.toStoredMessage(type, storagePath)
        storedMessageDao.insert(storedMessage)
        return storedMessage
    }

    private fun RAMFMessage.toStoredMessage(type: MessageType, storagePath: String): StoredMessage {
        val recipientAddress = MessageAddress.of(recipientAddress)
        return StoredMessage(
            recipientAddress = recipientAddress,
            recipientType = recipientAddress.type,
            senderAddress = PrivateMessageAddress(senderPrivateAddress),
            messageId = MessageId(messageId),
            messageType = type,
            creationTimeUtc = creationTime,
            expirationTimeUtc = Date(creationTime.time + ttl * 1000),
            size = StorageSize(payload.size.toLong()),
            storagePath = storagePath
        )
    }
}
