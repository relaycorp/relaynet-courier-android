package tech.relaycorp.courier.data.preference

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.test.AppTestProvider.flowSharedPreferences
import tech.relaycorp.courier.test.AppTestProvider.testDispatcher

class StoragePreferencesTest {
    private val preferences = StoragePreferences { flowSharedPreferences }

    @Before
    fun setUp() {
        flowSharedPreferences.clear()
    }

    @After
    fun tearDown() {
        flowSharedPreferences.clear()
    }

    @Test
    fun maxStorage() =
        runTest(testDispatcher) {
            assertEquals(
                StoragePreferences.DEFAULT_MAX_STORAGE_SIZE,
                preferences.getMaxStorageSize().first(),
            )

            val newSize = StorageSize(100)
            preferences.setMaxStorageSize(newSize)

            assertEquals(
                newSize,
                preferences.getMaxStorageSize().first(),
            )
        }
}
