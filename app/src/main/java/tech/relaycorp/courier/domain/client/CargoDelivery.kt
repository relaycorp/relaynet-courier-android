package tech.relaycorp.courier.domain.client

import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.data.network.CogRPCClient
import tech.relaycorp.courier.domain.DeleteMessage
import javax.inject.Inject

class CargoDelivery
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) {

    suspend fun deliver() {
        getCargoesToDeliver()
            .groupByRecipient()
            .entries
            .forEach { (recipientAddress, cargoes) ->
                val cogRPCClient = CogRPCClient.build(recipientAddress.value)
                cogRPCClient
                    .deliverCargo(cargoes.toCogRPCMessages())
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

    private suspend fun List<StoredMessage>.toCogRPCMessages() =
        map {
            CogRPCClient.MessageDelivery(
                localId = it.uniqueMessageId.value,
                data = diskRepository.readMessage(it.storagePath)
            )
        }

    private suspend fun CogRPCClient.MessageDeliveryAck.deleteCorrespondingCargo() =
        UniqueMessageId.from(localId).let {
            deleteMessage.delete(it.senderPrivateAddress, it.messageId)
        }
}
