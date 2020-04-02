package tech.relaycorp.courier.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_settings.deleteData
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.set
import javax.inject.Inject

class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<SettingsViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(SettingsViewModel::class.java)
    }

    private var deleteDataDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_settings)
        setupNavigation(R.drawable.ic_close)

        deleteData.setOnClickListener { openDeleteDataDialog() }

        viewModel
            .deleteDataEnabled()
            .onEach { deleteData.set(it) }
            .launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteDataDialog?.dismiss()
        deleteDataDialog = null
    }

    private fun openDeleteDataDialog() {
        deleteDataDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_delete_dialog_title)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteDataClicked() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
