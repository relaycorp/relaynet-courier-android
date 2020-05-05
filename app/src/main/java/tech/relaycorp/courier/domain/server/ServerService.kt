package tech.relaycorp.courier.domain.server

import tech.relaycorp.cogrpc.server.CogRPCServer
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.relaynet.CargoDeliveryRequest
import java.io.InputStream
import javax.inject.Inject

class ServerService
@Inject constructor(
    private val storeMessage: StoreMessage,
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) : CogRPCServer.Service {

    private val messagesSentForCollection = mutableMapOf<String, StoredMessage>()

    override suspend fun collectCargo(ccaSerialized: ByteArray): Iterable<CargoDeliveryRequest> {
        val ccaMessage = storeMessage.storeCCA(ccaSerialized) ?: return emptyList()
        val messages = storedMessageDao
            .getByRecipientAddressAndMessageType(ccaMessage.senderAddress, MessageType.Cargo)
        val messagesWithId = messages
            .map { StoredMessage.generateLocalId() to it }
            .toMap()
        messagesSentForCollection.putAll(messagesWithId)
        return messagesWithId.toRequests()
    }

    override suspend fun processCargoCollectionAck(localId: String) {
        messagesSentForCollection[localId]
            ?.let { message -> deleteMessage.delete(message) }
            ?: logger.warning("Ack with unknown id '$localId'")
    }

    override suspend fun deliverCargo(cargoSerialized: InputStream) =
        storeMessage.storeCargo(cargoSerialized) != null

    private suspend fun Map<String, StoredMessage>.toRequests() =
        mapNotNull { (localId, message) -> buildRequest(localId, message) }

    private suspend fun buildRequest(localId: String, message: StoredMessage) =
        readMessage(message)
            ?.let { data ->
                CargoDeliveryRequest(
                    localId = localId,
                    cargoSerialized = data
                )
            }

    private suspend fun readMessage(message: StoredMessage) =
        try {
            diskRepository.readMessage(message.storagePath)
        } catch (e: MessageDataNotFoundException) {
            null
        }
}
