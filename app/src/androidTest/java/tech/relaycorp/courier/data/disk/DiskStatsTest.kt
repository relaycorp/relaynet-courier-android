package tech.relaycorp.courier.data.disk

import org.junit.Assert.assertTrue
import org.junit.Test

internal class DiskStatsTest {

    private val diskStats = DiskStats()

    @Test
    internal fun basicCheck() {
        assertTrue(diskStats.totalStorage > 0)
        assertTrue(diskStats.availableStorage > 0)
        assertTrue(diskStats.availableStorage < diskStats.totalStorage)
    }
}
