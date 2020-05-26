package tech.relaycorp.courier.ui.settings

import android.os.Bundle
import com.mikepenz.aboutlibraries.ui.LibsActivity
import tech.relaycorp.courier.R

class LicensesActivity : LibsActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close)
        supportActionBar?.elevation = 0f
    }
}
