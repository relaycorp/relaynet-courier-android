package tech.relaycorp.courier.ui

import android.os.Bundle
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.common_app_bar.appBar
import kotlinx.android.synthetic.main.common_app_bar.toolbar
import tech.relaycorp.courier.App
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToPadding
import tech.relaycorp.courier.ui.common.MessageManager

abstract class BaseActivity : AppCompatActivity() {

    private val app get() = applicationContext as App
    val component by lazy { app.component.activityComponent() }

    protected val messageManager by lazy { MessageManager(this) }

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

    protected fun setupNavigation(
        @DrawableRes icon: Int = R.drawable.ic_close,
        clickListener: (() -> Unit) = { finish() }
    ) {
        toolbar?.setNavigationIcon(icon)
        toolbar?.setNavigationOnClickListener { clickListener.invoke() }
    }
}
