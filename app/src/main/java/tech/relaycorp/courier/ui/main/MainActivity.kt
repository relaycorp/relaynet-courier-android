package tech.relaycorp.courier.ui.main

import android.os.Bundle
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.R
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.common.di.ViewModelFactory
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.databinding.ActivityMainBinding
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

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        component.inject(this)
        setTitle(R.string.main_title)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.innerContainer.addSystemWindowInsetToPadding(bottom = true)

        binding.settings.setOnClickListener { openSettings() }
        binding.syncPeopleButton.setOnClickListener { openSyncWithPeople() }
        binding.syncInternetButton.setOnClickListener { openSyncWithInternet() }

        viewModel
            .storageUsage()
            .onEach { updateStorageUsage(it) }
            .launchIn(lifecycleScope)

        viewModel
            .lowStorageMessageIsVisible()
            .onEach { binding.lowStorageMessage.isVisible = it }
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
                    getString(R.string.main_deleted_expired_messages, it.format(this)),
                )
            }
            .launchIn(lifecycleScope)
    }

    private fun updateStorageUsage(usage: StorageUsage) {
        binding.storageProgress.progress = usage.percentage
        binding.storageValues.text =
            getString(
                R.string.main_storage_usage_values,
                usage.usedByApp.format(this),
                usage.actualMax.format(this),
            )
    }

    private fun updateSyncPeopleState(state: MainViewModel.SyncPeopleState) {
        val isEnabled = state is MainViewModel.SyncPeopleState.Enabled
        binding.syncPeopleLayout.isEnabled = isEnabled
        binding.syncPeopleButton.isEnabled = isEnabled
        binding.syncPeopleMessage.setText(
            if (isEnabled) R.string.sync_people_enabled else R.string.sync_people_disabled,
        )

        binding.hotspotLabel.isVisible = isEnabled
        binding.hotspotIcon.isVisible = isEnabled
        if (state is MainViewModel.SyncPeopleState.Enabled) {
            val isHotspotEnabled = state.hotspotState == WifiHotspotState.Enabled
            binding.hotspotIcon.isActivated = isHotspotEnabled
            binding.hotspotLabel.setText(
                if (isHotspotEnabled) R.string.hotspot_on else R.string.hotspot_off,
            )
        }
    }

    private fun updateSyncInternetState(state: MainViewModel.SyncInternetState) {
        val isEnabled = state is MainViewModel.SyncInternetState.Enabled
        binding.syncInternetLayout.isEnabled = isEnabled
        binding.syncInternetButton.isEnabled = isEnabled
        binding.syncInternetMessage.setText(
            when (state) {
                is MainViewModel.SyncInternetState.Enabled -> R.string.sync_internet_enabled
                is MainViewModel.SyncInternetState.Disabled.Offline -> R.string.sync_internet_disabled_offline
                is MainViewModel.SyncInternetState.Disabled.AlreadySynced -> R.string.sync_internet_disabled_synced
                is MainViewModel.SyncInternetState.Disabled.NoData -> R.string.sync_internet_disabled_no_data
            },
        )

        binding.internetLabel.isVisible = isEnabled
        binding.internetIcon.isVisible = isEnabled
        binding.internetIcon.isActivated = isEnabled
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
