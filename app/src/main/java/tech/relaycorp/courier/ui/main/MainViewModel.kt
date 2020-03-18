package tech.relaycorp.courier.ui.main

import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.collect
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    private val internetConnectionObserver: InternetConnectionObserver
) : BaseViewModel() {

    private val syncModeChannel = ConflatedBroadcastChannel<SyncMode>()
    val syncMode get() = syncModeChannel.asFlow()

    init {
        io {
            internetConnectionObserver
                .observe()
                .collect {
                    syncModeChannel.send(
                        when (it) {
                            InternetConnection.Online -> SyncMode.Internet
                            InternetConnection.Offline -> SyncMode.People
                        }
                    )
                }
        }
    }

    enum class SyncMode {
        People, Internet
    }
}
