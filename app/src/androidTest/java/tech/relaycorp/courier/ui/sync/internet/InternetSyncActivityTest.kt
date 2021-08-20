package tech.relaycorp.courier.ui.sync.internet

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.courier.R
import tech.relaycorp.courier.test.BaseActivityTestRule

class InternetSyncActivityTest {

    @JvmField
    @Rule
    val activityTestRule = BaseActivityTestRule(InternetSyncActivity::class)

    @Test
    fun showsDeliveringMessage() {
        assertDisplayed(R.id.stateMessage)
    }
}
