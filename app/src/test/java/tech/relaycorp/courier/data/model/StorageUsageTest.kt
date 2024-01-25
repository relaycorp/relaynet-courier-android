package tech.relaycorp.courier.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class StorageUsageTest {
    @Test
    internal fun percentage() {
        assertEquals(0, StorageUsage(StorageSize(0), StorageSize(10)).percentage)
        assertEquals(100, StorageUsage(StorageSize(10), StorageSize(10)).percentage)
        assertEquals(50, StorageUsage(StorageSize(499), StorageSize(1000)).percentage)
    }

    @Test
    internal fun available() {
        assertEquals(
            StorageSize(10),
            StorageUsage(StorageSize(0), StorageSize(10)).available,
        )
        assertEquals(
            StorageSize(5),
            StorageUsage(StorageSize(0), StorageSize(10), StorageSize(5)).available,
        )
    }

    @Test
    internal fun isLowOnSpace() {
        assertFalse(
            StorageUsage(
                StorageSize(0),
                StorageSize(StorageUsage.LOW_THRESHOLD.bytes + 1),
            ).isLowOnSpace,
        )
        assertTrue(
            StorageUsage(
                StorageSize(0),
                StorageSize(StorageUsage.LOW_THRESHOLD.bytes - 1),
            ).isLowOnSpace,
        )
    }
}
