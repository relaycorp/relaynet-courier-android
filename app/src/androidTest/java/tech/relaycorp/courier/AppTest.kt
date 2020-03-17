package tech.relaycorp.courier

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import tech.relaycorp.courier.test.AppTestProvider.app

@RunWith(AndroidJUnit4::class)
class AppTest {
    @Test
    fun mode() {
        assertEquals(App.Mode.Test, app.mode)
    }
}
