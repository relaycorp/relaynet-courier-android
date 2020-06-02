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
        val ccaMessageResult = storeMessage.storeCCA(ccaSerialized)
        if (ccaMessageResult !is StoreMessage.Result.Success) return emptyList()
        val ccaMessage =
            (ccaMessageResult as? StoreMessage.Result.Success)?.message ?: return emptyList()

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
        when (storeMessage.storeCargo(cargoSerialized)) {
            is StoreMessage.Result.Success -> CogRPCServer.DeliverResult.Successful
            is StoreMessage.Result.Error.NoSpaceAvailable -> CogRPCServer.DeliverResult.UnavailableStorage
            is StoreMessage.Result.Error.Invalid -> CogRPCServer.DeliverResult.Invalid
            is StoreMessage.Result.Error.Malformed -> CogRPCServer.DeliverResult.Malformed
        }

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
