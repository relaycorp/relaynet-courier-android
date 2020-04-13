package tech.relaycorp.courier.domain.server

import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.courier.domain.client.UniqueMessageId
import tech.relaycorp.relaynet.CargoRelay
import tech.relaycorp.relaynet.CargoRelayServer
import javax.inject.Inject

class ServerConnectionService
@Inject constructor(
    private val storeMessage: StoreMessage,
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) : CargoRelayServer.ConnectionService {

    override suspend fun collectCargo(cca: CargoRelay.MessageReceived): Iterable<CargoRelay.MessageDelivery> {
        val ccaMessage = storeMessage.storeCCA(cca.data) ?: return emptyList()
        return storedMessageDao
            .getByRecipientAddressAndMessageType(ccaMessage.senderAddress, MessageType.Cargo)
            .toCogRPCMessages()
    }

    override suspend fun processCargoCollectionAck(ack: CargoRelay.MessageDeliveryAck) {
        UniqueMessageId.from(ack.localId).let {
            deleteMessage.delete(it.senderPrivateAddress, it.messageId)
        }
    }

    override suspend fun deliverCargo(cargo: CargoRelay.MessageReceived) {
        storeMessage.storeCargo(cargo.data)
    }

    private suspend fun List<StoredMessage>.toCogRPCMessages() =
        mapNotNull { it.toCogRPCMessage() }

    private suspend fun StoredMessage.toCogRPCMessage() =
        readMessage(this)
            ?.let { data ->
                CargoRelay.MessageDelivery(
                    localId = uniqueMessageId.value,
                    data = data
                )
            }

    private suspend fun readMessage(message: StoredMessage) =
        try {
            diskRepository.readMessage(message.storagePath)
        } catch (e: MessageDataNotFoundException) {
            null
        }
}
