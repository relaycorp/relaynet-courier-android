package tech.relaycorp.courier.domain

import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PrivateMessageAddress
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.relaynet.Cargo
import tech.relaycorp.relaynet.CargoCollectionAuthorization
import tech.relaycorp.relaynet.RAMFMessage
import tech.relaycorp.relaynet.RAMFMessageMalformedException
import java.io.InputStream
import java.util.Date
import javax.inject.Inject

class StoreMessage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val getStorageUsage: GetStorageUsage,
    private val cargoDeserializer: ((@JvmSuppressWildcards ByteArray) -> Cargo),
    private val ccaDeserializer: ((@JvmSuppressWildcards ByteArray) -> CargoCollectionAuthorization)
) {

    suspend fun storeCargo(cargoInputStream: InputStream): StoredMessage? {
        val cargoBytes = cargoInputStream.readBytes()
        val cargo = try {
            cargoDeserializer(cargoBytes)
        } catch (e: RAMFMessageMalformedException) {
            logger.warning("Malformed Cargo received")
            return null
        }
        if (!cargo.isValid()) {
            logger.warning("Invalid Cargo received")
            return null
        }

        return storeMessage(MessageType.Cargo, cargo, cargoBytes)
    }

    suspend fun storeCCA(ccaSerialized: ByteArray): StoredMessage? {
        val cca = try {
            ccaDeserializer.invoke(ccaSerialized)
        } catch (e: RAMFMessageMalformedException) {
            logger.warning("Malformed CCA received")
            return null
        }
        if (!cca.isValid()) {
            logger.warning("Invalid CCA received")
            return null
        }

        return storeMessage(MessageType.CCA, cca, ccaSerialized)
    }

    private suspend fun storeMessage(
        type: MessageType,
        message: RAMFMessage,
        data: ByteArray
    ): StoredMessage? {
        val dataSize = StorageSize(data.size.toLong())
        if (!checkForAvailableSpace(dataSize)) return null

        val storagePath = diskRepository.writeMessage(data)
        val storedMessage = message.toStoredMessage(type, storagePath, dataSize)
        storedMessageDao.insert(storedMessage)
        return storedMessage
    }

    private suspend fun checkForAvailableSpace(dataSize: StorageSize) =
        getStorageUsage.get().available >= dataSize

    private fun RAMFMessage.toStoredMessage(
        type: MessageType,
        storagePath: String,
        dataSize: StorageSize
    ): StoredMessage {
        val recipientAddress = MessageAddress.of(recipientAddress)
        return StoredMessage(
            recipientAddress = recipientAddress,
            recipientType = recipientAddress.type,
            senderAddress = PrivateMessageAddress(senderPrivateAddress),
            messageId = MessageId(messageId),
            messageType = type,
            creationTimeUtc = creationTime,
            expirationTimeUtc = Date(creationTime.time + ttl.toLong() * 1000),
            size = dataSize,
            storagePath = storagePath
        )
    }
}
