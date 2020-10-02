package tech.relaycorp.courier.data.preference

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.test.AppTestProvider.flowSharedPreferences
import javax.inject.Provider

class StoragePreferencesTest {

    private val preferences = StoragePreferences(Provider { flowSharedPreferences })

    @Before
    fun setUp() {
        flowSharedPreferences.clear()
    }

    @After
    fun tearDown() {
        flowSharedPreferences.clear()
    }

    @Test
    fun maxStorage() = runBlocking {
        assertEquals(
            StoragePreferences.DEFAULT_MAX_STORAGE_SIZE,
            preferences.getMaxStorageSize().first()
        )

        val newSize = StorageSize(100)
        preferences.setMaxStorageSize(newSize)

        assertEquals(
            newSize,
            preferences.getMaxStorageSize().first()
        )
    }
}
