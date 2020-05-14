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
import tech.relaycorp.relaynet.messages.Cargo
import tech.relaycorp.relaynet.messages.CargoCollectionAuthorization
import java.time.ZonedDateTime

internal class StoreMessageTest {

    private val storedMessageDao = mock<StoredMessageDao>()
    private val diskRepository = mock<DiskRepository>()
    private val getStorageUsage = mock<GetStorageUsage>()

    private val subject = StoreMessage(storedMessageDao, diskRepository, getStorageUsage)

    @BeforeEach
    internal fun setUp() {
        runBlocking {
            whenever(getStorageUsage.get())
                .thenReturn(StorageUsage(StorageSize(0), StorageSize(Long.MAX_VALUE)))
            whenever(diskRepository.writeMessage(any())).thenReturn("")
        }
    }

    @Test
    internal fun `store message with just enough space`() = runBlockingTest {
        val message = RAMFMessageFactory.buildCargoSerialized()

        val storage = StorageUsage(StorageSize.ZERO, StorageSize(message.size.toLong()))
        whenever(getStorageUsage.get()).thenReturn(storage)

        assertNotNull(subject.storeCargo(message.inputStream()))
        verify(diskRepository).writeMessage(any())
        verify(storedMessageDao).insert(any())
    }

    @Test
    internal fun `do not store message if no space is available`() = runBlockingTest {
        val noStorageSpace = StorageUsage(StorageSize.ZERO, StorageSize.ZERO)
        whenever(getStorageUsage.get()).thenReturn(noStorageSpace)

        assertNull(subject.storeCargo(RAMFMessageFactory.buildCargoSerialized().inputStream()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `do not store malformed cargo`() = runBlockingTest {
        assertNull(subject.storeCargo("Not really a cargo".byteInputStream()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `do not store well-formed yet invalid cargo`() = runBlockingTest {
        // Use a cargo that expired the day before
        val invalidCargo = Cargo(
            RAMFMessageFactory.recipientAddress,
            "payload".toByteArray(),
            RAMFMessageFactory.senderCertificate,
            creationDate = ZonedDateTime.now().minusDays(1),
            ttl = 1
        )

        val invalidCargoSerialized =
            invalidCargo.serialize(RAMFMessageFactory.senderKeyPair.private)
        assertNull(subject.storeCargo(invalidCargoSerialized.inputStream()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `do not store malformed CCA`() = runBlockingTest {
        assertNull(subject.storeCCA("Not a RAMF message".toByteArray()))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `do not store well-formed yet invalid CCA`() = runBlockingTest {
        // Use a CCA that expired the day before
        val invalidCCA = CargoCollectionAuthorization(
            RAMFMessageFactory.recipientAddress,
            "payload".toByteArray(),
            RAMFMessageFactory.senderCertificate,
            creationDate = ZonedDateTime.now().minusDays(1),
            ttl = 1
        )

        assertNull(subject.storeCCA(invalidCCA.serialize(RAMFMessageFactory.senderKeyPair.private)))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }

    @Test
    internal fun `store valid CCA`() = runBlockingTest {
        assertNotNull(subject.storeCCA(RAMFMessageFactory.buildCCASerialized()))
        verify(diskRepository).writeMessage(any())
        verify(storedMessageDao).insert(any())
    }
}
