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
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.preference.StoragePreferences

internal class ObserveStorageUsageTest {

    private val storedMessageDao = mock<StoredMessageDao>()
    private val storagePreferences = mock<StoragePreferences>()
    private val diskStats = mock<DiskStats>()
    private val subject = ObserveStorageUsage(storedMessageDao, storagePreferences, diskStats)

    @Test
    internal fun `observe when there is enough space`() = runBlockingTest {
        whenever(storedMessageDao.observeFullSize()).thenReturn(flowOf(1))
        whenever(storagePreferences.getMaxStoragePercentage()).thenReturn(flowOf(10))
        whenever(diskStats.totalStorage).thenReturn(1_000_000)
        whenever(diskStats.observeAvailableStorage()).thenReturn(flowOf(999_999))

        assertEquals(
            StorageUsage(1, 100_000),
            subject.observe().first()
        )
    }

    @Test
    internal fun `observe when there is less space than configured`() = runBlockingTest {
        whenever(storedMessageDao.observeFullSize()).thenReturn(flowOf(1))
        whenever(storagePreferences.getMaxStoragePercentage()).thenReturn(flowOf(10))
        whenever(diskStats.totalStorage).thenReturn(1_000_000)
        whenever(diskStats.observeAvailableStorage()).thenReturn(flowOf(10_000))

        assertEquals(
            StorageUsage(1, 10_001),
            subject.observe().first()
        )
    }
}
