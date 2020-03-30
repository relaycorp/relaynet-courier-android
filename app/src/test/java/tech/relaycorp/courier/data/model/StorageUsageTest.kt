package tech.relaycorp.courier.data.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class StorageUsageTest {
    @Test
    internal fun percentage() {
        assertEquals(0, StorageUsage(0, 10).percentage)
        assertEquals(100, StorageUsage(10, 10).percentage)
        assertEquals(50, StorageUsage(499, 1000).percentage)
    }
}
