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
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.ramf.RAMFException
import tech.relaycorp.relaynet.ramf.RAMFMessage
import java.io.InputStream
import java.util.Date
import javax.inject.Inject

class StoreMessage
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val getStorageUsage: GetStorageUsage
) {

    suspend fun storeCargo(cargoInputStream: InputStream): StoredMessage? {
        val cargoBytes = cargoInputStream.readBytes()
        val cargo = try {
            Cargo.deserialize(cargoBytes)
        } catch (exc: RAMFException) {
            logger.warning("Malformed Cargo received: ${exc.message}")
            return null
        }

        try {
            cargo.validate()
        } catch (exc: RAMFException) {
            logger.warning("Invalid cargo received: ${exc.message}")
            return null
        }

        return storeMessage(MessageType.Cargo, cargo, cargoBytes)
    }

    suspend fun storeCCA(ccaSerialized: ByteArray): StoredMessage? {
        val cca = try {
            CargoCollectionAuthorization.deserialize(ccaSerialized)
        } catch (exc: RAMFException) {
            logger.warning("Malformed CCA received: ${exc.message}")
            return null
        }

        try {
            cca.validate()
        } catch (exc: RAMFException) {
            logger.warning("Invalid CCA received: ${exc.message}")
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
            senderAddress = PrivateMessageAddress(senderCertificate.subjectPrivateAddress),
            messageId = MessageId(id),
            messageType = type,
            creationTimeUtc = Date.from(creationDate.toInstant()),
            expirationTimeUtc = Date.from(expiryDate.toInstant()),
            size = dataSize,
            storagePath = storagePath
        )
    }
}
