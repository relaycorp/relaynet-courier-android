package tech.relaycorp.courier.ui.main

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.schibsted.spain.barista.assertion.BaristaVisibilityAssertions.assertDisplayed
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import tech.relaycorp.courier.R

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @JvmField
    @Rule
    val activityTestRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun title() {
        assertDisplayed(R.string.main_title)
    }
}
