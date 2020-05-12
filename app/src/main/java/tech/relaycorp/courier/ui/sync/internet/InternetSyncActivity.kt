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
import kotlinx.android.synthetic.main.activity_internet_sync.animation
import kotlinx.coroutines.flow.launchIn
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
                val isDone = it == PublicSync.State.Finished || it == PublicSync.State.Error
                stop.isVisible = !isDone
                close.isVisible = isDone
                if (it != PublicSync.State.Finished && it != PublicSync.State.Error) {
                    animation.startLoopingAvd(R.drawable.ic_sync_animation)
                } else {
                    animation.stopLoopingAvd()
                }
            }
            .launchIn(lifecycleScope)

        viewModel
            .finish
            .onEach { finish() }
            .launchIn(lifecycleScope)
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, InternetSyncActivity::class.java)
    }
}
