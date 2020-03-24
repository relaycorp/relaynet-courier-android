package tech.relaycorp.courier.ui.sync.people

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_people_sync.clientsConnected
import kotlinx.android.synthetic.main.activity_people_sync.close
import kotlinx.android.synthetic.main.activity_people_sync.stateMessage
import kotlinx.android.synthetic.main.activity_people_sync.stop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.domain.PrivateSync
import tech.relaycorp.courier.ui.BaseActivity
import javax.inject.Inject

class PeopleSyncActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<PeopleSyncViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(PeopleSyncViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_people_sync)

        stop.setOnClickListener { viewModel.stopClicked() }
        close.setOnClickListener { finish() }

        viewModel
            .state
            .onEach {
                stateMessage.text = it.toString()
                val isDone = it == PrivateSync.State.Stopped
                stop.isVisible = !isDone
                close.isVisible = isDone
            }
            .launchIn(lifecycleScope)

        viewModel
            .clientsConnected
            .onEach {
                clientsConnected.text = it.toString()
            }
            .launchIn(lifecycleScope)

        viewModel
            .finish
            .onEach { finish() }
            .launchIn(lifecycleScope)
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, PeopleSyncActivity::class.java)
    }
}
