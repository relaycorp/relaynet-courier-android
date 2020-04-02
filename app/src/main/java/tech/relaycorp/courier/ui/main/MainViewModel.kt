package tech.relaycorp.courier.ui.main

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.domain.ObserveStorageUsage
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    internetConnectionObserver: InternetConnectionObserver,
    observeStorageUsage: ObserveStorageUsage
) : BaseViewModel() {

    private val syncMode = BehaviorChannel<SyncMode>()
    fun syncMode() = syncMode.asFlow()

    private val storageUsage = BehaviorChannel<StorageUsage>()
    fun storageUsage() = storageUsage.asFlow()

    init {
        internetConnectionObserver
            .observe()
            .onEach {
                syncMode.send(
                    when (it) {
                        InternetConnection.Online -> SyncMode.Internet
                        InternetConnection.Offline -> SyncMode.People
                    }
                )
            }
            .launchIn(ioScope)

        observeStorageUsage
            .observe()
            .onEach(storageUsage::send)
            .launchIn(ioScope)
    }

    enum class SyncMode {
        People, Internet
    }
}
