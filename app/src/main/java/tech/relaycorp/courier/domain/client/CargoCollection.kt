package tech.relaycorp.courier.domain.client

import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.data.network.CogRPCClient
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import javax.inject.Inject

class CargoCollection
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val storeMessage: StoreMessage,
    private val deleteMessage: DeleteMessage,
    private val diskRepository: DiskRepository
) {

    suspend fun collect() {
        getCCAs()
            .forEach { cca ->
                val cogRPCClient = CogRPCClient.build(cca.recipientAddress.value)
                cogRPCClient
                    .collectCargo(cca.toCogRPCMessage())
                    .collect { storeMessage.storeCargo(it.data) }
                deleteMessage.delete(cca)
            }
    }

    private suspend fun getCCAs() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            MessageAddress.Type.Public,
            MessageType.Cargo
        )

    private suspend fun StoredMessage.toCogRPCMessage() =
        CogRPCClient.MessageDelivery(
            localId = uniqueMessageId.value,
            data = diskRepository.readMessage(storagePath)
        )
}
