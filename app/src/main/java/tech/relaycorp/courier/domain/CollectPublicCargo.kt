package tech.relaycorp.courier.domain

import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.data.network.CogRPC
import javax.inject.Inject

class CollectPublicCargo
@Inject constructor(
    private val storedMessageDao: StoredMessageDao,
    private val cogRPC: CogRPC,
    private val storeMessage: StoreMessage,
    private val deleteMessage: DeleteMessage,
    private val diskRepository: DiskRepository
) {

    suspend fun collect() {
        getCCAs()
            .forEach { cca ->
                cogRPC
                    .collectCargo(cca.recipientAddress.value, cca.toCogRPCMessage())
                    .collect {
                        storeMessage.storeCargo(
                            cca.recipientAddress,
                            MessageAddress.Type.Private,
                            it.data
                        )
                    }
                deleteMessage.delete(cca)
            }
    }

    private suspend fun getCCAs() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            MessageAddress.Type.Public,
            MessageType.Cargo
        )

    private fun StoredMessage.toCogRPCMessage() =
        CogRPC.MessageDelivery(
            senderAddress = senderAddress.value,
            messageId = messageId.value,
            data = diskRepository.readMessage(storagePath)
        )
}
