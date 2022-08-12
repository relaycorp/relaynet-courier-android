package tech.relaycorp.courier.domain.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient
import tech.relaycorp.relaynet.messages.Recipient
import tech.relaycorp.relaynet.testing.pki.KeyPairSet
import tech.relaycorp.relaynet.wrappers.nodeId

internal class CargoDeliveryTest {

    private val clientBuilder = mock<CogRPCClient.Builder>()
    private val storedMessageDao = mock<StoredMessageDao>()
    private val diskRepository = mock<DiskRepository>()
    private val deleteMessage = mock<DeleteMessage>()

    private val client = mock<CogRPCClient>()
    private val resolver = mock<InternetAddressResolver>()

    private val internetGatewayAddress = "example.com"
    private val publicGatewayTargetURL = "https://cogrpc.example.com:443"

    private val cargo = StoredMessageFactory.build(
        Recipient(KeyPairSet.INTERNET_GW.public.nodeId, internetGatewayAddress),
    )

    private val subject = CargoDelivery(
        clientBuilder, storedMessageDao, diskRepository, deleteMessage
    )

    @BeforeEach
    internal fun setUp() = runTest {
        whenever(resolver.resolve(internetGatewayAddress)).thenReturn(publicGatewayTargetURL)

        whenever(clientBuilder.build(eq(publicGatewayTargetURL), any(), any())).thenReturn(client)
        whenever(diskRepository.readMessage(any())).thenReturn { "".byteInputStream() }
    }

    @Test
    fun `deliver cargo successfully`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                (inv.arguments[0] as Iterable<CargoDeliveryRequest>)
                    .map { it.localId }
                    .asFlow()
            }

        subject.deliver(resolver)

        verify(client).deliverCargo(any())
        verify(deleteMessage).delete(eq(cargo))
    }

    @Test
    internal fun `deliver when unknown ack is received ignore it`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                flowOf(
                    (inv.arguments[0] as Iterable<CargoDeliveryRequest>).first().localId,
                    "unknown_ack"
                )
            }

        subject.deliver(resolver)

        verify(deleteMessage, times(1)).delete(any())
    }

    @Test
    fun `deliverToRecipient throws exception when cargo was not acknowledged`() = runTest {
        val cargo2 = cargo.copy(messageId = MessageId("id"))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                flowOf((inv.arguments[0] as Iterable<CargoDeliveryRequest>).first().localId)
            }

        assertThrows<CargoDelivery.IncompleteDeliveryException> {
            runBlocking {
                subject.deliverToRecipient(internetGatewayAddress, listOf(cargo, cargo2), resolver)
            }
        }
    }

    @Test
    fun `Failing to resolve DNS should be ignored`() = runTest {
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo))
        whenever(resolver.resolve(internetGatewayAddress))
            .thenThrow(InternetAddressResolutionException("Whoops"))

        subject.deliver(resolver)

        verify(client, never()).deliverCargo(any())
    }

    @Test
    fun `deliver to multiple recipients even if one fails`() = runTest {
        val internetGatewayAddress2 = "other.$internetGatewayAddress"
        whenever(resolver.resolve(internetGatewayAddress2)).thenReturn(publicGatewayTargetURL)
        val cargo2 = cargo.copy(recipientAddress = internetGatewayAddress2)
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo, cargo2))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                flowOf(
                    (inv.arguments[0] as Iterable<CargoDeliveryRequest>).first().localId,
                    "unknown_ack"
                )
            }

        subject.deliver(resolver)

        verify(client, times(2)).deliverCargo(any())
    }
}
