package tech.relaycorp.courier.ui.main

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import javax.inject.Inject
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.BaseActivity

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
    }
}
