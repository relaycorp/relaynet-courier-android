package tech.relaycorp.courier.ui.main

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    internetConnectionObserver: InternetConnectionObserver
) : BaseViewModel() {

    private val syncModeChannel = ConflatedBroadcastChannel<SyncMode>()
    val syncMode get() = syncModeChannel.asFlow()

    init {
        internetConnectionObserver
            .observe()
            .onEach {
                syncModeChannel.send(
                    when (it) {
                        InternetConnection.Online -> SyncMode.Internet
                        InternetConnection.Offline -> SyncMode.People
                    }
                )
            }
            .launchIn(ioScope)
    }

    enum class SyncMode {
        People, Internet
    }
}
