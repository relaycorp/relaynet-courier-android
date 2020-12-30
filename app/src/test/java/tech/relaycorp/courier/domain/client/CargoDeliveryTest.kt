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
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageId
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.data.model.PublicAddressResolutionException
import tech.relaycorp.courier.data.model.PublicMessageAddress
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import tech.relaycorp.doh.DoHClient
import tech.relaycorp.relaynet.CargoDeliveryRequest
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient

internal class CargoDeliveryTest {

    private val clientBuilder = mock<CogRPCClient.Builder>()
    private val storedMessageDao = mock<StoredMessageDao>()
    private val diskRepository = mock<DiskRepository>()
    private val deleteMessage = mock<DeleteMessage>()

    private val client = mock<CogRPCClient>()
    private val dohClient = mock<DoHClient>()

    private val publicGatewayURL = "https://example.com"
    private val publicGatewayTargetURL = "https://cogrpc.example.com:443"
    private val publicGatewayAddress = mock<PublicMessageAddress>()

    private val subject = CargoDelivery(
        clientBuilder, storedMessageDao, diskRepository, deleteMessage
    )

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        whenever(publicGatewayAddress.resolve(dohClient)).thenReturn(publicGatewayTargetURL)
        whenever(publicGatewayAddress.publicValue).thenReturn(publicGatewayURL)
        whenever(publicGatewayAddress.type).thenCallRealMethod()

        whenever(clientBuilder.build(eq(publicGatewayTargetURL), any(), any())).thenReturn(client)
        whenever(diskRepository.readMessage(any())).thenReturn { "".byteInputStream() }
    }

    @Test
    fun `deliver cargo successfully`() = runBlockingTest {
        val cargo = StoredMessageFactory.build(publicGatewayAddress)
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                (inv.arguments[0] as Iterable<CargoDeliveryRequest>)
                    .map { it.localId }
                    .asFlow()
            }

        subject.deliver(dohClient)

        verify(client).deliverCargo(any())
        verify(deleteMessage).delete(eq(cargo))
    }

    @Test
    internal fun `deliver when unknown ack is received ignore it`() = runBlockingTest {
        val cargo = StoredMessageFactory.build(publicGatewayAddress)
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

        subject.deliver(dohClient)

        verify(deleteMessage, times(1)).delete(any())
    }

    @Test
    fun `deliverToRecipient throws exception when cargo was not acknowledged`() = runBlockingTest {
        val cargo1 = StoredMessageFactory.build(publicGatewayAddress)
        val cargo2 = cargo1.copy(messageId = MessageId("id"))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                flowOf((inv.arguments[0] as Iterable<CargoDeliveryRequest>).first().localId)
            }

        assertThrows<CargoDelivery.IncompleteDeliveryException> {
            runBlockingTest {
                subject.deliverToRecipient(publicGatewayAddress, listOf(cargo1, cargo2), dohClient)
            }
        }
    }

    @Test
    fun `Failing to resolve DNS should be ignored`() = runBlockingTest {
        val cargo = StoredMessageFactory.build(publicGatewayAddress)
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo))
        whenever(publicGatewayAddress.resolve(dohClient))
            .thenThrow(PublicAddressResolutionException("Whoops"))

        subject.deliver(dohClient)

        verify(client, never()).deliverCargo(any())
    }

    @Test
    fun `deliver to multiple recipients even if one fails`() = runBlockingTest {
        val cargo1 = StoredMessageFactory.build(publicGatewayAddress)
        val publicGatewayAddress2 = mock<PublicMessageAddress>()
        whenever(publicGatewayAddress2.publicValue).thenReturn("https://another-gw.com")
        whenever(publicGatewayAddress2.type).thenCallRealMethod()
        whenever(publicGatewayAddress2.resolve(dohClient)).thenReturn(publicGatewayTargetURL)
        val cargo2 = StoredMessageFactory.build(publicGatewayAddress2)
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.Cargo)))
            .thenReturn(listOf(cargo1, cargo2))
        whenever(client.deliverCargo(any()))
            .thenAnswer { inv ->
                @Suppress("UNCHECKED_CAST")
                flowOf(
                    (inv.arguments[0] as Iterable<CargoDeliveryRequest>).first().localId,
                    "unknown_ack"
                )
            }

        subject.deliver(dohClient)

        verify(client, times(2)).deliverCargo(any())
    }
}
