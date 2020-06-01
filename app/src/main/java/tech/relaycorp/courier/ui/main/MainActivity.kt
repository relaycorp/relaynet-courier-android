package tech.relaycorp.courier.ui.main

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.stationhead.android.shared.viewmodel.ViewModelFactory
import kotlinx.android.synthetic.main.activity_main.hotspotIcon
import kotlinx.android.synthetic.main.activity_main.hotspotLabel
import kotlinx.android.synthetic.main.activity_main.innerContainer
import kotlinx.android.synthetic.main.activity_main.internetIcon
import kotlinx.android.synthetic.main.activity_main.internetLabel
import kotlinx.android.synthetic.main.activity_main.lowStorageMessage
import kotlinx.android.synthetic.main.activity_main.settings
import kotlinx.android.synthetic.main.activity_main.storageProgress
import kotlinx.android.synthetic.main.activity_main.storageValues
import kotlinx.android.synthetic.main.activity_main.syncInternetButton
import kotlinx.android.synthetic.main.activity_main.syncInternetLayout
import kotlinx.android.synthetic.main.activity_main.syncInternetMessage
import kotlinx.android.synthetic.main.activity_main.syncPeopleButton
import kotlinx.android.synthetic.main.activity_main.syncPeopleLayout
import kotlinx.android.synthetic.main.activity_main.syncPeopleMessage
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.ui.BaseActivity
import tech.relaycorp.courier.ui.common.Insets.addSystemWindowInsetToPadding
import tech.relaycorp.courier.ui.common.format
import tech.relaycorp.courier.ui.settings.SettingsActivity
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
        setTitle(R.string.main_title)
        setContentView(R.layout.activity_main)
        innerContainer.addSystemWindowInsetToPadding(bottom = true)

        settings.setOnClickListener { openSettings() }
        syncPeopleButton.setOnClickListener { openSyncWithPeople() }
        syncInternetButton.setOnClickListener { openSyncWithInternet() }

        viewModel
            .storageUsage()
            .onEach { updateStorageUsage(it) }
            .launchIn(lifecycleScope)

        viewModel
            .lowStorageMessageIsVisible()
            .onEach { lowStorageMessage.isVisible = it }
            .launchIn(lifecycleScope)

        viewModel
            .syncPeopleState()
            .onEach { updateSyncPeopleState(it) }
            .launchIn(lifecycleScope)

        viewModel
            .syncInternetState()
            .onEach { updateSyncInternetState(it) }
            .launchIn(lifecycleScope)

        viewModel
            .expiredMessagesDeleted()
            .onEach {
                messageManager.showMessage(
                    getString(R.string.main_deleted_expired_messages, it.format(this))
                )
            }
            .launchIn(lifecycleScope)
    }

    private fun updateStorageUsage(usage: StorageUsage) {
        storageProgress.progress = usage.percentage
        storageValues.text = getString(
            R.string.main_storage_usage_values,
            usage.usedByApp.format(this),
            usage.actualMax.format(this)
        )
    }

    private fun updateSyncPeopleState(state: MainViewModel.SyncPeopleState) {
        val isEnabled = state is MainViewModel.SyncPeopleState.Enabled
        syncPeopleLayout.isEnabled = isEnabled
        syncPeopleButton.isEnabled = isEnabled
        syncPeopleMessage.setText(
            if (isEnabled) R.string.sync_people_enabled else R.string.sync_people_disabled
        )

        hotspotLabel.isVisible = isEnabled
        hotspotIcon.isVisible = isEnabled
        if (state is MainViewModel.SyncPeopleState.Enabled) {
            val isHotspotEnabled = state.hotspotState == WifiHotspotState.Enabled
            hotspotIcon.isActivated = isHotspotEnabled
            hotspotLabel.setText(
                if (isHotspotEnabled) R.string.hotspot_on else R.string.hotspot_off
            )
        }
    }

    private fun updateSyncInternetState(state: MainViewModel.SyncInternetState) {
        val isEnabled = state is MainViewModel.SyncInternetState.Enabled
        syncInternetLayout.isEnabled = isEnabled
        syncInternetButton.isEnabled = isEnabled
        syncInternetMessage.setText(
            when (state) {
                is MainViewModel.SyncInternetState.Enabled -> R.string.sync_internet_enabled
                is MainViewModel.SyncInternetState.Disabled.Offline -> R.string.sync_internet_disabled_offline
                is MainViewModel.SyncInternetState.Disabled.AlreadySynced -> R.string.sync_internet_disabled_synced
                is MainViewModel.SyncInternetState.Disabled.NoData -> R.string.sync_internet_disabled_no_data
            }
        )

        internetLabel.isVisible = isEnabled
        internetIcon.isVisible = isEnabled
        internetIcon.isActivated = isEnabled
    }

    private fun openSettings() {
        startActivity(SettingsActivity.getIntent(this))
    }

    private fun openSyncWithInternet() {
        startActivity(InternetSyncActivity.getIntent(this))
    }

    private fun openSyncWithPeople() {
        startActivity(PeopleSyncActivity.getIntent(this))
    }
}
