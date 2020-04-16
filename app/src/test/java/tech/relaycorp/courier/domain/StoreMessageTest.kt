package tech.relaycorp.courier.domain

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskRepository
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage

internal class StoreMessageTest {

    private val storedMessageDao = mock<StoredMessageDao>()
    private val diskRepository = mock<DiskRepository>()
    private val getStorageUsage = mock<GetStorageUsage>()
    private val subject = StoreMessage(storedMessageDao, diskRepository, getStorageUsage)

    @Test
    internal fun `store message`() = runBlockingTest {
        val messageSize = 10
        val message = ByteArray(messageSize).inputStream()

        val storage = StorageUsage(StorageSize(0), StorageSize(messageSize.toLong()))
        whenever(getStorageUsage.get()).thenReturn(storage)
        whenever(diskRepository.writeMessage(any())).thenReturn("")

        assertNotNull(subject.storeCargo(message))
        verify(diskRepository).writeMessage(any())
        verify(storedMessageDao).insert(any())
    }

    @Test
    internal fun `do not store message if no space is available`() = runBlockingTest {
        val messageSize = 10
        val message = ByteArray(messageSize).inputStream()

        val fullStorage = StorageUsage(StorageSize(1_000), StorageSize(1_000))
        whenever(getStorageUsage.get()).thenReturn(fullStorage)

        assertNull(subject.storeCargo(message))
        verify(diskRepository, never()).writeMessage(any())
        verify(storedMessageDao, never()).insert(any())
    }
}
