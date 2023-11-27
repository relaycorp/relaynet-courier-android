package tech.relaycorp.courier.domain.client

import java.io.InputStream
import java.util.logging.Level
import javax.inject.Inject
import tech.relaycorp.cogrpc.okhttp.OkHTTPChannelBuilderProvider
import tech.relaycorp.courier.common.Logging.logger
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.disk.MessageDataNotFoundException
import tech.relaycorp.courier.data.model.GatewayType
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient

class CargoCollection
@Inject constructor(
    private val clientBuilder: CogRPCClient.Builder,
    private val storedMessageDao: StoredMessageDao,
    private val storeMessage: StoreMessage,
    private val deleteMessage: DeleteMessage,
    private val diskRepository: DiskRepository
) {

    suspend fun collect(resolver: InternetAddressResolver) {
        getCCAs()
            .forEach { cca ->
                try {
                    collectAndStoreCargoForCCA(cca, resolver)
                    deleteCCA(cca)
                } catch (e: CogRPCClient.CogRPCException) {
                    logger.log(Level.WARNING, "Cargo collection error", e)
                } catch (e: InternetAddressResolutionException) {
                    logger.log(Level.WARNING, "Failed to resolve ${cca.recipientAddress}", e)
                }
            }
    }

    private suspend fun getCCAs() =
        storedMessageDao.getByRecipientTypeAndMessageType(
            GatewayType.Internet,
            MessageType.CCA
        )

    @Throws(CogRPCClient.CogRPCException::class)
    private suspend fun collectAndStoreCargoForCCA(
        cca: StoredMessage,
        resolver: InternetAddressResolver
    ) {
        val resolvedAddress = resolver.resolve(cca.recipientAddress)
        val client = clientBuilder.build(
            resolvedAddress,
            OkHTTPChannelBuilderProvider.Companion::makeBuilder
        )
        try {
            client
                .collectCargo(cca.getSerializedInputStream())
                .collect { storeCargo(it) }
        } catch (e: MessageDataNotFoundException) {
            logger.log(Level.WARNING, "CCA data could not found on disk", e)
        } catch (e: CogRPCClient.CCARefusedException) {
            logger.log(Level.WARNING, "CCA refused")
        } finally {
            client.close()
        }
    }

    private suspend fun storeCargo(data: InputStream) =
        storeMessage.storeCargo(data, GatewayType.Private)

    private suspend fun deleteCCA(cca: StoredMessage) =
        deleteMessage.delete(cca)

    @Throws(MessageDataNotFoundException::class)
    private suspend fun StoredMessage.getSerializedInputStream() =
        diskRepository.readMessage(storagePath)
}
