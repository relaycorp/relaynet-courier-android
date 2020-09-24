package tech.relaycorp.courier.domain.client

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.MessageType
import tech.relaycorp.courier.domain.DeleteMessage
import tech.relaycorp.courier.domain.StoreMessage
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import tech.relaycorp.relaynet.cogrpc.client.CogRPCClient

internal class CargoCollectionTest {

    private val clientBuilder = mock<CogRPCClient.Builder>()
    private val storedMessageDao = mock<StoredMessageDao>()
    private val storeMessage = mock<StoreMessage>()
    private val deleteMessage = mock<DeleteMessage>()
    private val diskRepository = mock<DiskRepository>()

    private val client = mock<CogRPCClient>()

    private val subject = CargoCollection(
        clientBuilder, storedMessageDao, storeMessage, deleteMessage, diskRepository
    )

    @BeforeEach
    internal fun setUp() = runBlockingTest {
        whenever(clientBuilder.build(any(), any(), any())).thenReturn(client)
        whenever(diskRepository.readMessage(any())).thenReturn { "".byteInputStream() }
    }

    @Test
    internal fun `collect with CCA successfully`() = runBlockingTest {
        val cca = StoredMessageFactory.build()
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        val serializedCargo = buildSerializedCargo()
        whenever(client.collectCargo(any())).thenReturn(flowOf(serializedCargo))

        subject.collect()

        verify(storeMessage).storeCargo(eq(serializedCargo))
        verify(deleteMessage).delete(eq(cca))
    }

    @Test
    internal fun `collect with CCA refused deletes CCA`() = runBlockingTest {
        val cca = StoredMessageFactory.build()
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        whenever(client.collectCargo(any())).thenReturn(flow { throw CogRPCClient.CCARefusedException() })

        subject.collect()

        verify(deleteMessage).delete(eq(cca))
    }

    @Test
    internal fun `collect with unhandled exception`() = runBlockingTest {
        val cca = StoredMessageFactory.build()
        whenever(storedMessageDao.getByRecipientTypeAndMessageType(any(), eq(MessageType.CCA)))
            .thenReturn(listOf(cca))
        whenever(client.collectCargo(any())).thenReturn(flow { throw CogRPCClient.CogRPCException() })

        subject.collect()

        verify(storeMessage, never()).storeCargo(any())
        verify(deleteMessage, never()).delete(any())
    }

    private fun buildSerializedCargo() = "".byteInputStream()
}
