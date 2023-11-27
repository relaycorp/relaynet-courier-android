package tech.relaycorp.courier.ui.sync.people

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.common.di.ViewModelFactory
import tech.relaycorp.courier.databinding.ActivityPeopleSyncBinding
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToMargin
import tech.relaycorp.courier.ui.common.startLoopingAvd
import tech.relaycorp.courier.ui.common.stopLoopingAvd
import javax.inject.Inject

class PeopleSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<PeopleSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(PeopleSyncViewModel::class.java)
    }

    private lateinit var binding: ActivityPeopleSyncBinding

    private var stopConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPeopleSyncBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.stop.addSystemWindowInsetToMargin(bottom = true)
        binding.close.addSystemWindowInsetToMargin(bottom = true)

        binding.stop.setOnClickListener { viewModel.stopClicked() }
        binding.close.setOnClickListener { finish() }

        viewModel
            .state()
            .onEach { state ->
                binding.syncMessage.setText(state.toSyncMessageRes())

                binding.clientsConnected.isInvisible =
                    state !is PeopleSyncViewModel.State.Syncing.HadFirstClient
                binding.clientsConnected.text = state.clientsConnectedValue.toString()

                binding.stop.isInvisible = state !is PeopleSyncViewModel.State.Syncing
                binding.close.isVisible = state == PeopleSyncViewModel.State.Error

                when {
                    state is PeopleSyncViewModel.State.Syncing.HadFirstClient && state.clientsConnected > 0 ->
                        binding.animation.startLoopingAvd(R.drawable.ic_sync_animation_fast)
                    state is PeopleSyncViewModel.State.Syncing ->
                        binding.animation.startLoopingAvd(R.drawable.ic_sync_animation)
                    else ->
                        binding.animation.stopLoopingAvd()
                }
            }
            .onCompletion { binding.animation.stopLoopingAvd() }
            .launchIn(lifecycleScope)

        viewModel
            .openHotspotInstructions()
            .onEach { openHotspotInstructions() }
            .launchIn(lifecycleScope)

        viewModel
            .confirmStop()
            .onEach { showStopConfirmDialog() }
            .launchIn(lifecycleScope)

        viewModel
            .finish()
            .onEach { finish() }
            .launchIn(lifecycleScope)

        viewModel
            .finish()
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

    private fun openHotspotInstructions() {
        startActivity(HotspotInstructionsActivity.getIntent(this))
    }

    private fun showStopConfirmDialog() {
        stopConfirmDialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.sync_stop_confirm_title)
            .setMessage(R.string.sync_people_stop_confirm_message)
            .setPositiveButton(R.string.stop) { _, _ -> viewModel.confirmStopClicked() }
            .setNegativeButton(R.string.continue_, null)
            .show()
    }

    private fun PeopleSyncViewModel.State.toSyncMessageRes() =
        when (this) {
            PeopleSyncViewModel.State.Starting -> R.string.sync_people_starting
            PeopleSyncViewModel.State.Syncing.WaitingFirstClient -> R.string.sync_people_waiting_first_client
            is PeopleSyncViewModel.State.Syncing.HadFirstClient -> R.string.sync_people_syncing_some
            is PeopleSyncViewModel.State.Error -> R.string.sync_error
        }

    private val PeopleSyncViewModel.State.clientsConnectedValue
        get() =
            (this as? PeopleSyncViewModel.State.Syncing.HadFirstClient)?.clientsConnected ?: 0

    companion object {
        fun getIntent(context: Context) = Intent(context, PeopleSyncActivity::class.java)
    }
}
