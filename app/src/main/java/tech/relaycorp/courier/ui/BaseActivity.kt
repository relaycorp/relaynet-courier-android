package tech.relaycorp.courier.ui

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.appbar.AppBarLayout
import tech.relaycorp.courier.App
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToPadding
import tech.relaycorp.courier.ui.common.MessageManager

abstract class BaseActivity : AppCompatActivity() {

    private val app get() = applicationContext as App
    val component by lazy { app.component.activityComponent() }

    protected val messageManager by lazy { MessageManager(this) }

    private val appBar get() = findViewById<AppBarLayout?>(R.id.appBar)
    private val toolbar get() = findViewById<Toolbar?>(R.id.toolbar)
    private val toolbarTitle get() = findViewById<TextView?>(R.id.toolbarTitle)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup edge-to-edge UI
        window.decorView.systemUiVisibility =
            window.decorView.systemUiVisibility or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
    }

    override fun setContentView(view: View?) {
        super.setContentView(view)
        toolbarTitle?.text = title
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
