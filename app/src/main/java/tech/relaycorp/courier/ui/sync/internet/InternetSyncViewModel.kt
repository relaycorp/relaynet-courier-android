package tech.relaycorp.courier.ui.sync.internet

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.common.PublishFlow
import tech.relaycorp.courier.domain.PublicSync
import tech.relaycorp.courier.ui.BaseViewModel
import tech.relaycorp.courier.ui.common.Click
import tech.relaycorp.courier.ui.common.Finish
import javax.inject.Inject

class InternetSyncViewModel
    @Inject
    constructor(
        private val publicSync: PublicSync,
    ) : BaseViewModel() {
        // Inputs

        fun stopClicked() = stopClicks.tryEmit(Click)

        private val stopClicks = PublishFlow<Click>()

        // Outputs

        private val stateChannel = MutableStateFlow<PublicSync.State?>(null)
        val state get() = stateChannel.asStateFlow().filterNotNull()

        private val finishChannel = PublishFlow<Finish>()
        val finish get() = finishChannel.asSharedFlow()

        init {
            val syncJob =
                scope.launch {
                    publicSync.sync()
                }

            val syncStateJob =
                publicSync
                    .state()
                    .onEach { stateChannel.emit(it) }
                    .launchIn(scope)

            stopClicks
                .onEach {
                    syncStateJob.cancel()
                    syncJob.cancel()
                    finishChannel.emit(Finish)
                }
                .launchIn(scope)
        }
    }
