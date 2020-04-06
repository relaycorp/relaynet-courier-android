package tech.relaycorp.courier.ui.main

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteExpiredMessages
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    internetConnectionObserver: InternetConnectionObserver,
    getStorageUsage: GetStorageUsage,
    deleteExpiredMessages: DeleteExpiredMessages
) : BaseViewModel() {

    private val syncMode = BehaviorChannel<SyncMode>()
    fun syncMode() = syncMode.asFlow()

    private val storageUsage = BehaviorChannel<StorageUsage>()
    fun storageUsage() = storageUsage.asFlow()

    private val lowStorageMessageIsVisible = BehaviorChannel<Boolean>(false)
    fun lowStorageMessageIsVisible() = lowStorageMessageIsVisible.asFlow()

    private val expiredMessagesDeleted = BehaviorChannel<StorageSize>()
    fun expiredMessagesDeleted() = expiredMessagesDeleted.asFlow()

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

        getStorageUsage
            .observe()
            .onEach {
                storageUsage.send(it)
                lowStorageMessageIsVisible.send(it.isLowOnSpace)
            }
            .launchIn(ioScope)

        ioScope.launch {
            val messagesDeleted = deleteExpiredMessages.delete()
            if (messagesDeleted.any()) {
                expiredMessagesDeleted.send(messagesDeleted.sumMessageSize())
            }
        }
    }

    private fun List<StoredMessage>.sumMessageSize() =
        map { it.size }.reduce { acc, messageSize -> acc + messageSize }

    enum class SyncMode {
        People, Internet
    }
}
