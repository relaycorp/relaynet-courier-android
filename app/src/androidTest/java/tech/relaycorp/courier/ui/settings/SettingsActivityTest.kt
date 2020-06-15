package tech.relaycorp.courier.ui.settings

import com.google.android.material.slider.Slider
import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertDisabled
import com.schibsted.spain.barista.assertion.BaristaEnabledAssertions.assertEnabled
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertContains
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.courier.BuildConfig
import tech.relaycorp.courier.R
import tech.relaycorp.courier.data.database.StoredMessageDao
import tech.relaycorp.courier.data.preference.StoragePreferences
import tech.relaycorp.courier.test.BaseActivityTestRule
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
        slider.value = slider.valueFrom
        runBlocking {
            assertEquals(
                SettingsViewModel.MIN_STORAGE_SIZE,
                storagePreferences.getMaxStorageSize().first()
            )
        }
    }
}
