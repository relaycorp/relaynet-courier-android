package tech.relaycorp.courier.data.disk

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.relaycorp.courier.data.model.StorageSize

internal class DiskStatsTest {
    private val diskStats = DiskStats()

    @Test
    internal fun basicCheck() =
        runBlocking {
            assertTrue(diskStats.getTotalStorage() > StorageSize.ZERO)
            assertTrue(diskStats.getAvailableStorage() > StorageSize.ZERO)
            assertTrue(diskStats.getAvailableStorage() < diskStats.getTotalStorage())
        }
}
