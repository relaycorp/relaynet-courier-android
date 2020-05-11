package tech.relaycorp.courier.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.test.factory.RAMFMessageFactory
import tech.relaycorp.relaynet.Cargo
import tech.relaycorp.relaynet.RAMFMessageMalformedException
import tech.relaycorp.relaynet.issueGatewayCertificate
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import tech.relaycorp.relaynet.wrappers.generateRSAKeyPair
import java.time.ZonedDateTime

internal class StoreMessageTest {

    private val storedMessageDao = mock<StoredMessageDao>()
    private val diskRepository = mock<DiskRepository>()
    private val getStorageUsage = mock<GetStorageUsage>()
    private val cargoDeserializer = mock<((ByteArray) -> Cargo)>()

    private val subject = StoreMessage(
        storedMessageDao,
        diskRepository,
        getStorageUsage,
        cargoDeserializer
    )

    private val senderKeyPair = generateRSAKeyPair()
    private val senderCertificate = issueGatewayCertificate(
        senderKeyPair.public,
        senderKeyPair.private,
        ZonedDateTime.now().plusSeconds(10)
    )

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            whenever(cargoDeserializer.invoke(any())).thenReturn(RAMFMessageFactory.buildCargo())
            whenever(getStorageUsage.get())
                .thenReturn(StorageUsage(StorageSize(0), StorageSize(Long.MAX_VALUE)))
            whenever(diskRepository.writeMessage(any())).thenReturn("")
        }
    }

    @Test
    internal fun `store message with just enough space`() = runBlockingTest {
        val messageSize = 10
        val message = buildMessageStream(messageSize)

        val storage = StorageUsage(StorageSize.ZERO, StorageSize(messageSize.toLong()))
        whenever(getStorageUsage.get()).thenReturn(storage)

        assertNotNull(subject.storeCargo(message))
        verify(diskRepository).writeMessage(any())
        verify(storedMessageDao).insert(any())
    }

    @Test
    internal fun `do not store message if no space is available`() = runBlockingTest {
        val noStorageSpace = StorageUsage(StorageSize.ZERO, StorageSize.ZERO)
        whenever(getStorageUsage.get()).thenReturn(noStorageSpace)

        assertNull(subject.storeCargo(buildMessageStream()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `store cargo with malformed message`() = runBlockingTest {
        whenever(cargoDeserializer.invoke(any())).then {
            throw RAMFMessageMalformedException()
        }

        assertNull(subject.storeCargo(buildMessageStream()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `store cargo with invalid message`() = runBlockingTest {
        val cargo = mock<Cargo> {
            on { isValid() }.thenReturn(false)
        }
        whenever(cargoDeserializer.invoke(any())).thenReturn(cargo)

        assertNull(subject.storeCargo(buildMessageStream()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `store CCA with malformed message`() = runBlockingTest {
        assertNull(subject.storeCCA("Not a RAMF message".toByteArray()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `store well-formed yet invalid CCA`() = runBlockingTest {
        // Use a CCA that expired the day before
        val invalidCCA = CargoCollectionAuthorization(
            "https://example.com",
            "payload".toByteArray(),
            senderCertificate,
            creationDate = ZonedDateTime.now().minusDays(1),
            ttl = 1
        )

        assertNull(subject.storeCCA(invalidCCA.serialize(senderKeyPair.private)))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    private fun buildMessageData(size: Int = 1) = ByteArray(size)
    private fun buildMessageStream(size: Int = 1) = buildMessageData(size).inputStream()
}
