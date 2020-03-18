package tech.relaycorp.courier.ui.main

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_main.syncInternetLayout
import kotlinx.android.synthetic.main.activity_main.syncPeopleLayout
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.BaseActivity
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

        lifecycleScope.launch {
            viewModel
                .syncMode
                .collect {
                    syncPeopleLayout.isVisible = it == MainViewModel.SyncMode.People
                    syncInternetLayout.isVisible = it == MainViewModel.SyncMode.Internet
                }
        }
    }
}
