package tech.relaycorp.courier.ui.sync.people

import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.courier.R
import tech.relaycorp.courier.test.BaseActivityTestRule

class PeopleSyncActivityTest {
    @JvmField
    @Rule
    val activityTestRule = BaseActivityTestRule(PeopleSyncActivity::class)

    @Test
    fun opensInstructionsWithoutHotspot() {
        assertDisplayed(R.string.hotspot_instructions_disabled)
    }
}
