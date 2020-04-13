package tech.relaycorp.courier.domain.client

import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.relaynet.CargoRelay
import tech.relaycorp.relaynet.CargoRelayClient
import tech.relaycorp.courier.domain.DeleteMessage
import javax.inject.Inject

class CargoDelivery
@Inject constructor(
    private val cargoRelayClientBuilder: CargoRelayClient.Builder,
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) {

    suspend fun deliver() {
        getCargoesToDeliver()
            .groupByRecipient()
            .forEach { (recipientAddress, cargoes) ->
                cargoRelayClientBuilder
                    .build(recipientAddress.value)
                    .deliverCargo(cargoes.toCogRPCMessages())
                    .collect { deleteDeliveredCargo(it) }
            }
    }

    private suspend fun getCargoesToDeliver() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            MessageAddress.Type.Public,
            MessageType.Cargo
        )

    private suspend fun deleteDeliveredCargo(ack: CargoRelay.MessageDeliveryAck) =
        UniqueMessageId.from(ack.localId).let {
            deleteMessage.delete(it.senderPrivateAddress, it.messageId)
        }

    private fun List<StoredMessage>.groupByRecipient() =
        groupBy { it.recipientAddress }.entries

    private suspend fun Iterable<StoredMessage>.toCogRPCMessages() =
        mapNotNull {
            readMessage(it)
                ?.let { data ->
                    CargoRelay.MessageDelivery(
                        localId = it.uniqueMessageId.value,
                        data = data
                    )
                }
        }

    private suspend fun readMessage(message: StoredMessage) =
        try {
            diskRepository.readMessage(message.storagePath)
        } catch (e: MessageDataNotFoundException) {
            null
        }
}
