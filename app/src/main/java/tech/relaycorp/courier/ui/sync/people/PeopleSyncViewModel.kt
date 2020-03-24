package tech.relaycorp.courier.ui.sync.people

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.PublishChannel
import tech.relaycorp.courier.domain.PrivateSync
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import tech.relaycorp.courier.ui.common.Finish
import javax.inject.Inject

class PeopleSyncViewModel
@Inject constructor(
    private val privateSync: PrivateSync
) : BaseViewModel() {

    // Inputs

    fun stopClicked() = stopClicks.sendBlocking(Click)
    private val stopClicks = PublishChannel<Click>()

    // Outputs

    private val stateChannel = BehaviorChannel<PrivateSync.State>()
    val state get() = stateChannel.asFlow()

    private val clientsConnectedChannel = BehaviorChannel<Int>()
    val clientsConnected get() = clientsConnectedChannel.asFlow()

    private val finishChannel = PublishChannel<Finish>()
    val finish get() = finishChannel.asFlow()

    init {
        ioScope.launch {
            privateSync.startSync()
            clientsConnectedChannel.send(0)
        }

        privateSync
            .state()
            .onEach { stateChannel.send(it) }
            .launchIn(ioScope)

        privateSync
            .clientConnected()
            .onEach {
                clientsConnectedChannel.send((clientsConnectedChannel.valueOrNull ?: 0) + 1)
            }
            .launchIn(ioScope)

        stopClicks
            .asFlow()
            .onEach {
                privateSync.stopSync()
                finishChannel.send(Finish)
            }
            .launchIn(ioScope)
    }
}
