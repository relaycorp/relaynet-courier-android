package tech.relaycorp.courier.ui.sync.internet

import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.common.PublishChannel
import tech.relaycorp.courier.domain.PublicSync
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import javax.inject.Inject

class InternetSyncViewModel
@Inject constructor(
    private val publicSync: PublicSync
) : BaseViewModel() {

    // Inputs

    fun stopClicked() = stopClicks.sendBlocking(Click)
    private val stopClicks = PublishChannel<Click>()

    // Outputs

    private val stateChannel = BehaviorChannel<PublicSync.State>()
    val state get() = stateChannel.asFlow()

    private val errorsChannel = PublishChannel<Error>()
    val errors get() = errorsChannel.asFlow()

    init {
        val syncJob = ioScope.launch {
            publicSync.sync()
        }

        publicSync
            .state()
            .catch { errorsChannel.send(Error.Sync) }
            .onEach { stateChannel.send(it) }
            .launchIn(ioScope)

        stopClicks
            .asFlow()
            .onEach { syncJob.cancel() }
            .launchIn(ioScope)
    }

    enum class Error {
        Sync
    }
}
