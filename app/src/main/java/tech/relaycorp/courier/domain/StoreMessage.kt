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
import java.util.Date
import javax.inject.Inject

class StoreMessage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val getStorageUsage: GetStorageUsage
) {

    suspend fun storeCargo(cargo: Cargo) =
        storeMessage(cargo, MessageType.Cargo)

    suspend fun storeCCA(cca: CargoCollectionAuthorization) =
        storeMessage(cca, MessageType.CCA)

    private suspend fun storeMessage(message: RAMFMessage, type: MessageType): StoredMessage? {
        if (!checkForAvailableSpace(message)) return null

        val storagePath = diskRepository.writeMessage(message.payload)
        val storedMessage = message.toStoredMessage(type, storagePath)
        storedMessageDao.insert(storedMessage)
        return storedMessage
    }

    private suspend fun checkForAvailableSpace(message: RAMFMessage) =
        getStorageUsage.get().available.bytes >= message.payload.size

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
