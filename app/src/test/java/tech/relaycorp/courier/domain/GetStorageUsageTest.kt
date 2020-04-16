package tech.relaycorp.courier.domain

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.disk.DiskStats
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.preference.StoragePreferences

internal class GetStorageUsageTest {

    private val storedMessageDao = mock<StoredMessageDao>()
    private val storagePreferences = mock<StoragePreferences>()
    private val diskStats = mock<DiskStats>()
    private val subject = GetStorageUsage(storedMessageDao, storagePreferences, diskStats)

    @Test
    internal fun `observe when there is enough space`() = runBlockingTest {
        whenever(storedMessageDao.observeTotalSize()).thenReturn(flowOf(StorageSize(1)))
        whenever(storagePreferences.getMaxStorageSize()).thenReturn(flowOf(StorageSize(100_000)))
        whenever(diskStats.observeAvailableStorage()).thenReturn(flowOf(StorageSize(999_999)))

        assertEquals(
            StorageUsage(StorageSize(1), StorageSize(100_000), StorageSize(100_000)),
            subject.observe().first()
        )
    }

    @Test
    internal fun `observe when there is less space than configured`() = runBlockingTest {
        whenever(storedMessageDao.observeTotalSize()).thenReturn(flowOf(StorageSize(1)))
        whenever(storagePreferences.getMaxStorageSize()).thenReturn(flowOf(StorageSize(100_000)))
        whenever(diskStats.observeAvailableStorage()).thenReturn(flowOf(StorageSize(10_000)))

        assertEquals(
            StorageUsage(StorageSize(1), StorageSize(100_000), StorageSize(10_001)),
            subject.observe().first()
        )
    }
}
