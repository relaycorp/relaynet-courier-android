package tech.relaycorp.courier.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.common_app_bar.appBar
import kotlinx.android.synthetic.main.common_app_bar.toolbar
import tech.relaycorp.courier.App
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToPadding

abstract class BaseActivity : AppCompatActivity() {

    private val app get() = applicationContext as App
    val component by lazy { app.component.activityComponent() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup edge-to-edge UI
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    }

    override fun setContentView(layoutResID: Int) {
        super.setContentView(layoutResID)
        toolbar?.title = title
        appBar?.addSystemWindowInsetToPadding(top = true)
    }
}
