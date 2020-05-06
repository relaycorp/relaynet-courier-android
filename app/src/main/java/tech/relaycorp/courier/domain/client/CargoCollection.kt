package tech.relaycorp.courier.domain.client

import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.io.InputStream
import java.util.logging.Level
import javax.inject.Inject

class CargoCollection
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val storedMessageDao: StoredMessageDao,
    private val storeMessage: StoreMessage,
    private val deleteMessage: DeleteMessage,
    private val diskRepository: DiskRepository
) {

    suspend fun collect() {
        getCCAs()
            .forEach { cca ->
                collectAndStoreCargoForCCA(cca)
                deleteCCA(cca)
            }
    }

    private suspend fun getCCAs() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            MessageAddress.Type.Public,
            MessageType.CCA
        )

    private suspend fun collectAndStoreCargoForCCA(cca: StoredMessage) {
        val client = clientBuilder.build(cca.recipientAddress.value)
        try {
            client
                .collectCargo(cca.getSerializedInputStream())
                .collect { storeCargo(it) }
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "CCA data could not found on disk", e)
        } catch (e: CogRPCClient.CCARefusedError) {
            logger.log(Level.WARNING, "CCA refused")
        } finally {
            client.close()
        }
    }

    private suspend fun storeCargo(data: InputStream) =
        storeMessage.storeCargo(data)

    private suspend fun deleteCCA(cca: StoredMessage) =
        deleteMessage.delete(cca)

    @Throws(MessageDataNotFoundException::class)
    private suspend fun StoredMessage.getSerializedInputStream() =
        diskRepository.readMessage(storagePath)
}
