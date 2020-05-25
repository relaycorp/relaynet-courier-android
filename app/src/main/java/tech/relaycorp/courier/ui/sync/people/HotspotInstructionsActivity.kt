package tech.relaycorp.courier.ui.sync.people

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_hotspot_instructions.icon
import kotlinx.android.synthetic.main.activity_hotspot_instructions.openSettings
import kotlinx.android.synthetic.main.activity_hotspot_instructions.startSync
import kotlinx.android.synthetic.main.activity_hotspot_instructions.text
import kotlinx.android.synthetic.main.activity_hotspot_instructions.titleText
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.ui.BaseActivity
import javax.inject.Inject

class HotspotInstructionsActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelFactory<HotspotInstructionsViewModel>

    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory).get(HotspotInstructionsViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        component.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hotspot_instructions)
        setupNavigation()

        openSettings.setOnClickListener { openHotspotSettings() }
        startSync.setOnClickListener { openPeopleSync() }

        viewModel
            .state()
            .onEach { updateState(it) }
            .launchIn(lifecycleScope)
    }

    private fun updateState(state: HotspotInstructionsViewModel.State) {
        icon.isActivated = state == HotspotInstructionsViewModel.State.ReadyToSync
        titleText.setText(
            when (state) {
                HotspotInstructionsViewModel.State.ReadyToSync -> R.string.hotspot_instructions_enabled
                HotspotInstructionsViewModel.State.NotReadyToSync -> R.string.hotspot_instructions_disabled
            }
        )
        text.setText(
            when (state) {
                HotspotInstructionsViewModel.State.ReadyToSync -> R.string.hotspot_instructions_enabled_text
                HotspotInstructionsViewModel.State.NotReadyToSync -> R.string.hotspot_instructions_disabled_text
            }
        )
        openSettings.isVisible = state == HotspotInstructionsViewModel.State.NotReadyToSync
        startSync.isVisible = state == HotspotInstructionsViewModel.State.ReadyToSync
    }

    private fun openHotspotSettings() {
        startActivity(
            Intent(Intent.ACTION_MAIN, null)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setComponent(
                    ComponentName(
                        "com.android.settings",
                        "com.android.settings.TetherSettings"
                    )
                )
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openPeopleSync() {
        startActivity(PeopleSyncActivity.getIntent(this))
        finish()
    }

    companion object {
        fun getIntent(context: Context) = Intent(context, HotspotInstructionsActivity::class.java)
    }
}
