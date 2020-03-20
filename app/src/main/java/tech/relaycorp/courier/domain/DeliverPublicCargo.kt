package tech.relaycorp.courier.domain

import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.data.network.CogRPC
import javax.inject.Inject

class DeliverPublicCargo
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val cogRPC: CogRPC,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) {

    suspend fun deliver() {
        getCargoesToDeliver()
            .groupByRecipient()
            .entries
            .forEach { (recipientAddress, cargoes) ->
                cogRPC
                    .deliverCargo(
                        recipientAddress.value,
                        cargoes.toCogRPCMessages()
                    )
                    .collect {
                        it.deleteCorrespondingCargo()
                    }
            }
    }

    suspend fun getCargoesToDeliver() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            MessageAddress.Type.Public,
            MessageType.Cargo
        )

    private fun List<StoredMessage>.groupByRecipient() =
        groupBy { it.recipientAddress }

    private fun List<StoredMessage>.toCogRPCMessages() =
        map {
            CogRPC.MessageDelivery(
                senderAddress = it.senderAddress.value,
                messageId = it.messageId.value,
                data = diskRepository.readMessage(it.storagePath)
            )
        }

    private suspend fun CogRPC.MessageDeliveryAck.deleteCorrespondingCargo() =
        deleteMessage.delete(
            MessageAddress(senderAddress),
            MessageId(messageId)
        )
}
