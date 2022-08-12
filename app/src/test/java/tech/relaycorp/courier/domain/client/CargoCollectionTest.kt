package tech.relaycorp.courier.domain.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.GatewayType
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.nodeId

// TODO: Factor out code duplicated with CargoDeliveryTest
internal class CargoCollectionTest {

    private val clientBuilder = mock<CogRPCClient.Builder>()
    private val storedMessageDao = mock<StoredMessageDao>()
    private val storeMessage = mock<StoreMessage>()
    private val deleteMessage = mock<DeleteMessage>()
    private val diskRepository = mock<DiskRepository>()

    private val client = mock<CogRPCClient>()
    private val resolver = mock<InternetAddressResolver>()

    private val internetGatewayAddress = "example.com"
    private val publicGatewayTargetURL = "https://cogrpc.example.com:443"

    private val cca = StoredMessageFactory.build(
        Recipient(KeyPairSet.INTERNET_GW.public.nodeId, internetGatewayAddress),
    )

    private val subject = CargoCollection(
        clientBuilder, storedMessageDao, storeMessage, deleteMessage, diskRepository
    )

    @BeforeEach
    internal fun setUp() = runTest {
        whenever(resolver.resolve(internetGatewayAddress)).thenReturn(publicGatewayTargetURL)

        whenever(clientBuilder.build(eq(publicGatewayTargetURL), any(), any())).thenReturn(client)
        whenever(diskRepository.readMessage(any())).thenReturn { "".byteInputStream() }
    }

    @Test
    internal fun `collect with CCA successfully`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        val serializedCargo = buildSerializedCargo()
        whenever(client.collectCargo(any())).thenReturn(flowOf(serializedCargo))

        subject.collect(resolver)

        verify(storeMessage).storeCargo(eq(serializedCargo), eq(GatewayType.Private))
        verify(deleteMessage).delete(eq(cca))
    }

    @Test
    internal fun `collect with CCA refused deletes CCA`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        whenever(client.collectCargo(any())).thenReturn(flow { throw CogRPCClient.CCARefusedException() })

        subject.collect(resolver)

        verify(deleteMessage).delete(eq(cca))
    }

    @Test
    internal fun `collect with unhandled CogRPC exception`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        whenever(client.collectCargo(any())).thenReturn(flow { throw CogRPCClient.CogRPCException() })

        subject.collect(resolver)

        verify(storeMessage, never()).storeCargo(any(), any())
        verify(deleteMessage, never()).delete(any())
    }

    @Test
    fun `Failing to resolve DNS should be ignored`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        whenever(client.collectCargo(any()))
            .thenReturn(flow { throw InternetAddressResolutionException("Whoops") })

        subject.collect(resolver)

        verify(storeMessage, never()).storeCargo(any(), any())
        verify(deleteMessage, never()).delete(any())
    }

    private fun buildSerializedCargo() = "".byteInputStream()
}
