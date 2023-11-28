package tech.relaycorp.courier.ui.settings

import androidx.annotation.VisibleForTesting
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.PublishFlow
import tech.relaycorp.courier.data.disk.DiskStats
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.preference.StoragePreferences
import tech.relaycorp.courier.domain.DeleteAllStorage
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import tech.relaycorp.courier.ui.common.EnableState
import tech.relaycorp.courier.ui.common.toEnableState
import javax.inject.Inject

class SettingsViewModel
    @Inject
    constructor(
        getStorageUsage: GetStorageUsage,
        deleteAllStorage: DeleteAllStorage,
        storagePreferences: StoragePreferences,
        diskStats: DiskStats,
    ) : BaseViewModel() {
        // Inputs

        private val deleteDataClicks = PublishFlow<Click>()

        fun deleteDataClicked() = deleteDataClicks.tryEmit(Click)

        private val maxStorageChanged = PublishFlow<StorageSize>()

        fun maxStorageChanged(value: StorageSize) = maxStorageChanged.tryEmit(value)

        // Outputs

        private val deleteDataEnabled = MutableStateFlow<EnableState?>(null)

        fun deleteDataEnabled() = deleteDataEnabled.asStateFlow().filterNotNull()

        private val maxStorage = MutableStateFlow<StorageSize?>(null)

        fun maxStorage() = maxStorage.asStateFlow().filterNotNull()

        private val maxStorageBoundary = MutableStateFlow<SizeBoundary?>(null)

        fun maxStorageBoundary() = maxStorageBoundary.asStateFlow().filterNotNull()

        private val storageStats = MutableStateFlow<StorageStats?>(null)

        fun storageStats() = storageStats.asStateFlow().filterNotNull()

        init {
            getStorageUsage
                .observe()
                .onEach {
                    deleteDataEnabled.value = (!it.usedByApp.isZero).toEnableState()
                }
                .launchIn(scope)

            combine(
                getStorageUsage.observe(),
                diskStats.observeAvailableStorage(),
            ) { usage, available ->
                StorageStats(
                    used = usage.usedByApp,
                    usedPercentage = usage.percentage,
                    available = available,
                    total = diskStats.getTotalStorage(),
                )
            }
                .onEach { storageStats.value = it }
                .launchIn(scope)

            deleteDataClicks
                .onEach { deleteAllStorage.delete() }
                .launchIn(scope)

            storagePreferences
                .getMaxStorageSize()
                .onEach { maxStorage.value = it }
                .launchIn(scope)

            maxStorageChanged
                .onEach { storagePreferences.setMaxStorageSize(it) }
                .launchIn(scope)

            scope.launch {
                val totalStorageValue = diskStats.getTotalStorage()
                maxStorageBoundary.value =
                    SizeBoundary(
                        MIN_STORAGE_SIZE,
                        totalStorageValue,
                        STORAGE_SIZE_STEP,
                    )
            }
        }

        companion object {
            @VisibleForTesting
            val MIN_STORAGE_SIZE = StorageSize(100_000_000)

            @VisibleForTesting
            val STORAGE_SIZE_STEP = StorageSize(100_000_000)
        }
    }
