package tech.relaycorp.courier.data.preference

import kotlinx.coroutines.test.runBlockingTest
import org.junit.After
import org.junit.Test
import tech.relaycorp.courier.test.AppTestProvider.flowSharedPreferences
import tech.relaycorp.courier.test.test
import javax.inject.Provider

class StoragePreferencesTest {

    private val preferences = StoragePreferences(Provider { flowSharedPreferences })

    @After
    fun tearDown() {
        flowSharedPreferences.clear()
    }

    @Test
    fun maxStorage() = runBlockingTest {
        val observer = preferences.getMaxStoragePercentage().test(this)
        preferences.setMaxStoragePercentage(100)

        observer
            .assertValues(StoragePreferences.DEFAULT_MAX_STORAGE_PERCENTAGE, 100)
            .finish()
    }
}
