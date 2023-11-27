package tech.relaycorp.courier.ui.sync.internet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.common.di.ViewModelFactory
import tech.relaycorp.courier.databinding.ActivityInternetSyncBinding
import tech.relaycorp.courier.domain.PublicSync
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToMargin
import tech.relaycorp.courier.ui.common.startLoopingAvd
import tech.relaycorp.courier.ui.common.stopLoopingAvd
import javax.inject.Inject

class InternetSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<InternetSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(InternetSyncViewModel::class.java)
    }

    private lateinit var binding: ActivityInternetSyncBinding

    private var stopConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityInternetSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.stop.addSystemWindowInsetToMargin(bottom = true)
        binding.close.addSystemWindowInsetToMargin(bottom = true)

        binding.stop.setOnClickListener { showStopConfirmDialog() }
        binding.close.setOnClickListener { finish() }

        viewModel
            .state
            .onEach { binding.stateMessage.setText(it.toStringRes()) }
            .map { it != PublicSync.State.Finished && it != PublicSync.State.Error }
            .distinctUntilChanged()
            .onEach { isSyncing ->
                binding.stop.isVisible = isSyncing
                binding.close.isVisible = !isSyncing
                if (isSyncing) {
                    binding.animation.startLoopingAvd(R.drawable.ic_sync_animation)
                } else {
                    binding.animation.stopLoopingAvd()
                }
            }
            .onCompletion { binding.animation.stopLoopingAvd() }
            .launchIn(lifecycleScope)

        viewModel
            .finish
            .onEach { finish() }
            .launchIn(lifecycleScope)
    }

    override fun onBackPressed() {
        binding.stop.performClick()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (stopConfirmDialog?.isShowing == true) stopConfirmDialog?.dismiss()
        stopConfirmDialog = null
    }

    private fun showStopConfirmDialog() {
        stopConfirmDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sync_stop_confirm_title)
            .setMessage(R.string.sync_internet_stop_confirm_message)
            .setPositiveButton(R.string.stop) { _, _ -> viewModel.stopClicked() }
            .setNegativeButton(R.string.continue_, null)
            .show()
    }

    private fun PublicSync.State.toStringRes() =
        when (this) {
            PublicSync.State.DeliveringCargo -> R.string.sync_delivering_cargo
            PublicSync.State.Waiting -> R.string.sync_waiting
            PublicSync.State.CollectingCargo -> R.string.sync_collecting_cargo
            PublicSync.State.Finished -> R.string.sync_finished
            PublicSync.State.Error -> R.string.sync_error
        }

    companion object {
        fun getIntent(context: Context) = Intent(context, InternetSyncActivity::class.java)
    }
}
