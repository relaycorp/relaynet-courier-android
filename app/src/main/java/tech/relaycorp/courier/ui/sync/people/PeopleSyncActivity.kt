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
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_people_sync.animation
import kotlinx.android.synthetic.main.activity_people_sync.clientsConnected
import kotlinx.android.synthetic.main.activity_people_sync.close
import kotlinx.android.synthetic.main.activity_people_sync.stop
import kotlinx.android.synthetic.main.activity_people_sync.syncMessage
import kotlinx.android.synthetic.main.activity_people_sync.waitingFirstClient
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.startLoopingAvd
import tech.relaycorp.courier.ui.common.stopLoopingAvd
import javax.inject.Inject

class PeopleSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<PeopleSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(PeopleSyncViewModel::class.java)
    }

    private var stopConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people_sync)

        stop.setOnClickListener { viewModel.stopClicked() }
        close.setOnClickListener { finish() }

        viewModel
            .state()
            .onEach { state ->
                syncMessage.setText(state.toSyncMessageRes())

                waitingFirstClient.isVisible =
                    state == PeopleSyncViewModel.State.Syncing.WaitingFirstClient
                clientsConnected.isVisible =
                    state is PeopleSyncViewModel.State.Syncing.HadFirstClient
                clientsConnected.text = state.clientsConnectedValue.toString()

                stop.isInvisible = state !is PeopleSyncViewModel.State.Syncing
                val isDone = state == PeopleSyncViewModel.State.Error
                close.isVisible = isDone
                if (!isDone) {
                    animation.startLoopingAvd(R.drawable.ic_sync_animation)
                } else {
                    animation.stopLoopingAvd()
                }
            }
            .onCompletion { animation.stopLoopingAvd() }
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
        stop.performClick()
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
            PeopleSyncViewModel.State.Syncing.WaitingFirstClient -> R.string.sync_people_syncing
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
