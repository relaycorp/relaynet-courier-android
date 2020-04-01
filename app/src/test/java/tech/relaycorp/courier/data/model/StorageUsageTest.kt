package tech.relaycorp.courier.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StorageUsageTest {
    @Test
    internal fun percentage() {
        assertEquals(0, StorageUsage(StorageSize(0), StorageSize(10)).percentage)
        assertEquals(100, StorageUsage(StorageSize(10), StorageSize(10)).percentage)
        assertEquals(50, StorageUsage(StorageSize(499), StorageSize(1000)).percentage)
    }
}
