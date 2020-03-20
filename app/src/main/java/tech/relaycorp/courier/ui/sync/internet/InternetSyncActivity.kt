package tech.relaycorp.courier.ui.sync.internet

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_internet_sync.close
import kotlinx.android.synthetic.main.activity_internet_sync.stateMessage
import kotlinx.android.synthetic.main.activity_internet_sync.stop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.domain.PublicSync
import tech.relaycorp.courier.ui.BaseActivity
import javax.inject.Inject

class InternetSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<InternetSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(InternetSyncViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internet_sync)

        stop.setOnClickListener { viewModel.stopClicked() }
        close.setOnClickListener { finish() }

        viewModel
            .state
            .onEach {
                stateMessage.text = it.toString()
                stop.isVisible = it != PublicSync.State.Finished
                close.isVisible = it == PublicSync.State.Finished
            }
            .launchIn(lifecycleScope)

        viewModel
            .errors
            .onEach { showError(it) }
            .launchIn(lifecycleScope)
    }

    private fun showError(error: InternetSyncViewModel.Error) {
        messageManager.showError(
            when (error) {
                InternetSyncViewModel.Error.Sync -> R.string.sync_error
            }
        )
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, InternetSyncActivity::class.java)
    }
}
