package tech.relaycorp.courier.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.mikepenz.aboutlibraries.LibsBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import tech.relaycorp.courier.BuildConfig
import tech.relaycorp.courier.R
import tech.relaycorp.courier.common.di.ViewModelFactory
import tech.relaycorp.courier.databinding.ActivitySettingsBinding
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToPadding
import tech.relaycorp.courier.ui.common.format
import tech.relaycorp.courier.ui.common.set
import javax.inject.Inject

class SettingsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<SettingsViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(SettingsViewModel::class.java)
    }

    private lateinit var binding: ActivitySettingsBinding

    private var deleteDataDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation(R.drawable.ic_close)
        binding.innerContainer.addSystemWindowInsetToPadding(bottom = true)

        binding.version.text =
            getString(R.string.about_version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)

        binding.knowMore.setOnClickListener { openKnowMore() }
        binding.licenses.setOnClickListener { openLicenses() }
        binding.deleteData.setOnClickListener { openDeleteDataDialog() }

        viewModel
            .deleteDataEnabled()
            .onEach { binding.deleteData.set(it) }
            .launchIn(lifecycleScope)

        viewModel
            .storageStats()
            .onEach {
                binding.storageUsed.text = it.used.format(this)
                binding.storageAvailable.text = it.available.format(this)
                binding.storageTotal.text = it.total.format(this)
            }
            .launchIn(lifecycleScope)

        viewModel
            .maxStorageBoundary()
            .onEach { binding.storageMaxSlider.sizeBoundary = it }
            .launchIn(lifecycleScope)

        binding.storageMaxSlider.addOnChangeListener { _, _, _ ->
            viewModel.maxStorageChanged(binding.storageMaxSlider.size)
        }

        viewModel.maxStorage()
            .take(1)
            .onEach { binding.storageMaxSlider.size = it }
            .launchIn(lifecycleScope)

        viewModel
            .maxStorage()
            .onEach { binding.storageMaxValue.text = it.format(this) }
            .launchIn(lifecycleScope)
    }

    override fun onDestroy() {
        super.onDestroy()
        deleteDataDialog?.dismiss()
        deleteDataDialog = null
    }

    private fun openKnowMore() {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.relaynet_website))))
    }

    private fun openLicenses() {
        LibsBuilder()
            .withActivityTitle(getString(R.string.about_licenses))
            .withAboutIconShown(false)
            .withVersionShown(false)
            .withOwnLibsActivityClass(LicensesActivity::class.java)
            .withEdgeToEdge(true)
            .withFields(R.string::class.java.fields)
            .start(this)
    }

    private fun openDeleteDataDialog() {
        deleteDataDialog = AlertDialog.Builder(this)
            .setTitle(R.string.settings_clear_dialog_title)
            .setPositiveButton(R.string.delete) { _, _ -> viewModel.deleteDataClicked() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, SettingsActivity::class.java)
    }
}
