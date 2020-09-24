package tech.relaycorp.courier.ui.main

import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.courier.R
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.preference.StoragePreferences
import tech.relaycorp.courier.test.BaseActivityTestRule
import tech.relaycorp.courier.test.WaitAssertions.waitFor
import tech.relaycorp.courier.test.appComponent
import tech.relaycorp.courier.test.context
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import tech.relaycorp.courier.ui.common.format
import javax.inject.Inject

class MainActivityTest {

    @JvmField
    @Rule
    val activityTestRule = BaseActivityTestRule(MainActivity::class)

    @Inject
    lateinit var storagePreferences: StoragePreferences

    @Inject
    lateinit var storedMessageDao: StoredMessageDao

    @Before
    fun setUp() {
        appComponent.inject(this)
    }

    @Test
    fun title() {
        assertDisplayed(R.string.main_title)
    }

    @Test
    fun showsStorageValues() {
        val maxSize = StorageSize(100_000L)
        val currentSize = StorageSize(1_000L)
        GlobalScope.launch {
            storagePreferences.setMaxStorageSize(maxSize)
            storedMessageDao.insert(StoredMessageFactory.build().copy(size = currentSize))
        }

        waitFor {
            assertContains(maxSize.format(context))
            assertContains(currentSize.format(context))
        }
    }

    @Test
    fun syncDisabledWithoutData() {
        assertDisabled(R.id.syncInternetButton)
        assertDisplayed(R.string.sync_internet_disabled_no_data)
    }
}
