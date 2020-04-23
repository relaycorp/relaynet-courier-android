package tech.relaycorp.courier.domain.server

import tech.relaycorp.cogrpc.server.CogRPCServer
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.courier.domain.client.UniqueMessageId
import tech.relaycorp.relaynet.cogrpc.CogRPC
import javax.inject.Inject

class ServerService
@Inject constructor(
    private val storeMessage: StoreMessage,
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) : CogRPCServer.Service {

    override suspend fun collectCargo(cca: CogRPC.MessageReceived): Iterable<CogRPC.MessageDelivery> {
        val ccaMessage = storeMessage.storeCCA(cca.data) ?: return emptyList()
        return storedMessageDao
            .getByRecipientAddressAndMessageType(ccaMessage.senderAddress, MessageType.Cargo)
            .toCogRPCMessages()
    }

    override suspend fun processCargoCollectionAck(ack: CogRPC.MessageDeliveryAck) {
        UniqueMessageId.from(ack.localId).let {
            deleteMessage.delete(it.senderPrivateAddress, it.messageId)
        }
    }

    override suspend fun deliverCargo(cargo: CogRPC.MessageReceived) =
        storeMessage.storeCargo(cargo.data) != null

    private suspend fun List<StoredMessage>.toCogRPCMessages() =
        mapNotNull { it.toCogRPCMessage() }

    private suspend fun StoredMessage.toCogRPCMessage() =
        readMessage(this)
            ?.let { data ->
                CogRPC.MessageDelivery(
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
