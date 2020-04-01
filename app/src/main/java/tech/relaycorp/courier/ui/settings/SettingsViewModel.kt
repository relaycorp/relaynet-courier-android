package tech.relaycorp.courier.ui.settings

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.PublishChannel
import tech.relaycorp.courier.domain.DeleteAllStorage
import tech.relaycorp.courier.domain.ObserveStorageUsage
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import tech.relaycorp.courier.ui.common.EnableState
import tech.relaycorp.courier.ui.common.toEnableState
import javax.inject.Inject

class SettingsViewModel
@Inject constructor(
    observeStorageUsage: ObserveStorageUsage,
    deleteAllStorage: DeleteAllStorage
) : BaseViewModel() {

    // Inputs

    private val deleteDataClicks = PublishChannel<Click>()
    fun deleteDataClicked() = deleteDataClicks.sendBlocking(Click)

    // Outputs

    private val deleteDataEnabled = BehaviorChannel<EnableState>()
    fun deleteDataEnabled() = deleteDataEnabled.asFlow()

    init {
        observeStorageUsage
            .observe()
            .onEach { deleteDataEnabled.send((!it.usedByApp.isZero).toEnableState()) }
            .launchIn(ioScope)

        deleteDataClicks
            .asFlow()
            .onEach { deleteAllStorage.delete() }
            .launchIn(ioScope)
    }
}
