package tech.relaycorp.courier.ui.settings

import com.google.android.material.slider.Slider
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.adevinta.android.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertContains
import com.schibsted.spain.barista.rule.flaky.AllowFlaky
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.courier.BuildConfig
import tech.relaycorp.courier.R
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.preference.StoragePreferences
import tech.relaycorp.courier.test.BaseActivityTestRule
import tech.relaycorp.courier.test.WaitAssertions.suspendWaitFor
import tech.relaycorp.courier.test.appComponent
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import javax.inject.Inject

class SettingsActivityTest {

    @Rule
    @JvmField
    val testRule = BaseActivityTestRule(SettingsActivity::class, false)

    @Inject
    lateinit var storedMessageDao: StoredMessageDao

    @Inject
    lateinit var storagePreferences: StoragePreferences

    @Before
    fun setUp() {
        appComponent.inject(this)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun displaysVersion() {
        testRule.start()
        assertContains(BuildConfig.VERSION_CODE.toString())
        assertContains(BuildConfig.VERSION_NAME)
    }

    @Test
    fun clearButtonDisabledWithoutData() {
        testRule.start()
        assertDisabled(R.id.deleteData)
    }

    @Test
    @AllowFlaky(attempts = 3)
    fun clearButtonDisabledWithData() {
        runBlocking {
            storedMessageDao.insert(StoredMessageFactory.build())
        }
        testRule.start()
        assertEnabled(R.id.deleteData)
    }

    @Test
    fun setMaxStorageToMinimum() {
        val activity = testRule.start()
        val slider = activity.findViewById<Slider>(R.id.storageMaxSlider)
        runBlocking {
            suspendWaitFor {
                slider.value = slider.valueFrom
                assertEquals(
                    SettingsViewModel.MIN_STORAGE_SIZE,
                    storagePreferences.getMaxStorageSize().first()
                )
            }
        }
    }
}
