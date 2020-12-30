package tech.relaycorp.courier.domain.client

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.collect
import tech.relaycorp.cogrpc.okhttp.OkHTTPChannelBuilderProvider
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.MessageAddress
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PublicAddressResolutionException
import tech.relaycorp.courier.data.model.PublicMessageAddress
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import java.util.logging.Level
import javax.inject.Inject

class CargoDelivery
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val storedMessageDao: StoredMessageDao,
    private val diskRepository: DiskRepository,
    private val deleteMessage: DeleteMessage
) {

    suspend fun deliver(dohClient: DoHClient) {
        getCargoesToDeliver()
            .groupByRecipient()
            .forEach { (recipientAddress, cargoes) ->
                try {
                    deliverToRecipient(recipientAddress as PublicMessageAddress, cargoes, dohClient)
                } catch (e: IncompleteDeliveryException) {
                    logger.log(Level.WARNING, "Server failed to acknowledge one or more cargoes")
                } catch (e: CogRPCClient.CogRPCException) {
                    logger.log(Level.WARNING, "Cargo delivery error", e)
                } catch (e: PublicAddressResolutionException) {
                    logger.log(Level.WARNING, "Failed to resolve ${recipientAddress.value}", e)
                }
            }
    }

    private suspend fun getCargoesToDeliver() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            MessageAddress.Type.Public,
            MessageType.Cargo
        )

    private fun List<StoredMessage>.groupByRecipient() =
        groupBy { it.recipientAddress }.entries

    @Throws(IncompleteDeliveryException::class, CogRPCClient.CogRPCException::class)
    @VisibleForTesting
    internal suspend fun deliverToRecipient(
        recipientAddress: PublicMessageAddress,
        cargoes: List<StoredMessage>,
        dohClient: DoHClient
    ) {
        val cargoesWithId =
            cargoes.map { StoredMessage.generateLocalId() to it }.toMap().toMutableMap()
        val requests = cargoesWithId.toRequests()
        val client = clientBuilder.build(
            recipientAddress.resolve(dohClient),
            OkHTTPChannelBuilderProvider.Companion::makeBuilder
        )
        try {
            client
                .deliverCargo(requests)
                .collect { localId ->
                    cargoesWithId.remove(localId)
                        ?.let { message -> deleteMessage.delete(message) }
                        ?: logger.warning("Ack with unknown id '$localId'")
                }
        } finally {
            client.close()
        }
        if (cargoesWithId.any()) {
            throw IncompleteDeliveryException()
        }
    }

    private suspend fun Map<String, StoredMessage>.toRequests() =
        mapNotNull { (localId, message) ->
            readMessage(message)
                ?.let { data ->
                    CargoDeliveryRequest(
                        localId = localId,
                        cargoSerialized = data
                    )
                }
        }

    private suspend fun readMessage(message: StoredMessage) =
        try {
            diskRepository.readMessage(message.storagePath)
        } catch (e: MessageDataNotFoundException) {
            null
        }

    class IncompleteDeliveryException :
        Exception("Some delivered cargo was now acknowledge by the server")
}
