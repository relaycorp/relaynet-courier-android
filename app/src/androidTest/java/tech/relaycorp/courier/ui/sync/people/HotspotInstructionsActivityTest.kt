package tech.relaycorp.courier.ui.sync.people

import com.adevinta.android.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Rule
import org.junit.Test
import tech.relaycorp.courier.R
import tech.relaycorp.courier.test.BaseActivityTestRule

class HotspotInstructionsActivityTest {
    @JvmField
    @Rule
    val testRule = BaseActivityTestRule(HotspotInstructionsActivity::class)

    @Test
    fun whenHotspotDisabled() {
        assertDisplayed(R.string.hotspot_instructions_disabled)
        assertDisplayed(R.string.hotspot_instructions_disabled_text)
        assertDisplayed(R.string.hotspot_instructions_open_settings)
    }
}
