package tech.relaycorp.courier.ui.sync.internet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_internet_sync.animation
import kotlinx.android.synthetic.main.activity_internet_sync.close
import kotlinx.android.synthetic.main.activity_internet_sync.stateMessage
import kotlinx.android.synthetic.main.activity_internet_sync.stop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.domain.PublicSync
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.startLoopingAvd
import tech.relaycorp.courier.ui.common.stopLoopingAvd
import javax.inject.Inject

class InternetSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<InternetSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(InternetSyncViewModel::class.java)
    }

    private var stopConfirmDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internet_sync)

        stop.setOnClickListener { showStopConfirmDialog() }
        close.setOnClickListener { finish() }

        viewModel
            .state
            .onEach {
                stateMessage.setText(it.toStringRes())

                val isDone = it == PublicSync.State.Finished || it == PublicSync.State.Error
                stop.isVisible = !isDone
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
            .finish
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
