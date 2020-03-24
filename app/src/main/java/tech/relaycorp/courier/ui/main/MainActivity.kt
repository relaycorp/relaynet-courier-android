package tech.relaycorp.courier.ui.main

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_main.syncInternetLayout
import kotlinx.android.synthetic.main.activity_main.syncPeopleLayout
import kotlinx.android.synthetic.main.activity_main.syncWithInternet
import kotlinx.android.synthetic.main.activity_main.syncWithPeople
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.sync.internet.InternetSyncActivity
import tech.relaycorp.courier.ui.sync.people.PeopleSyncActivity
import javax.inject.Inject

class MainActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<MainViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setContentView(R.layout.activity_main)

        syncWithPeople.setOnClickListener { openSyncWithPeople() }
        syncWithInternet.setOnClickListener { openSyncWithInternet() }

        viewModel
            .syncMode
            .onEach {
                syncPeopleLayout.isVisible = it == MainViewModel.SyncMode.People
                syncInternetLayout.isVisible = it == MainViewModel.SyncMode.Internet
            }
            .launchIn(lifecycleScope)
    }

    private fun openSyncWithInternet() {
        startActivity(InternetSyncActivity.getIntent(this))
    }

    private fun openSyncWithPeople() {
        startActivity(PeopleSyncActivity.getIntent(this))
    }
}
