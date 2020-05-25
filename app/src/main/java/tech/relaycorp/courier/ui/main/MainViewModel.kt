package tech.relaycorp.courier.ui.main

import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateReceiver
import tech.relaycorp.courier.common.BehaviorChannel
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteExpiredMessages
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.domain.ObserveCCACount
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
@Inject constructor(
    internetConnectionObserver: InternetConnectionObserver,
    hotspotStateReceiver: WifiHotspotStateReceiver,
    getStorageUsage: GetStorageUsage,
    observeCCACount: ObserveCCACount,
    deleteExpiredMessages: DeleteExpiredMessages
) : BaseViewModel() {

    private val syncPeopleState = BehaviorChannel<SyncPeopleState>()
    fun syncPeopleState() = syncPeopleState.asFlow()

    private val syncInternetState = BehaviorChannel<SyncInternetState>()
    fun syncInternetState() = syncInternetState.asFlow()

    private val storageUsage = BehaviorChannel<StorageUsage>()
    fun storageUsage() = storageUsage.asFlow()

    private val lowStorageMessageIsVisible = BehaviorChannel(false)
    fun lowStorageMessageIsVisible() = lowStorageMessageIsVisible.asFlow()

    private val expiredMessagesDeleted = BehaviorChannel<StorageSize>()
    fun expiredMessagesDeleted() = expiredMessagesDeleted.asFlow()

    init {
        combine(
            internetConnectionObserver.observe(),
            hotspotStateReceiver.state()
        ) { internetConnection, hotspotState ->
            when (internetConnection) {
                InternetConnection.Offline -> SyncPeopleState.Enabled(hotspotState)
                InternetConnection.Online -> SyncPeopleState.Disabled
            }
        }
            .onEach(syncPeopleState::send)
            .launchIn(ioScope)

        combine(
            internetConnectionObserver.observe(),
            getStorageUsage.observe(),
            observeCCACount.observe()
        ) { internetConnection, storageUsage, ccaCount ->
            when (internetConnection) {
                InternetConnection.Online ->
                    when {
                        storageUsage.usedByApp.isZero -> SyncInternetState.Disabled.NoData
                        ccaCount == 0L -> SyncInternetState.Disabled.AlreadySynced
                        else -> SyncInternetState.Enabled
                    }
                InternetConnection.Offline -> SyncInternetState.Disabled.Offline
            }
        }
            .onEach(syncInternetState::send)
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

    sealed class SyncPeopleState {
        data class Enabled(val hotspotState: WifiHotspotState) : SyncPeopleState()
        object Disabled : SyncPeopleState()
    }

    sealed class SyncInternetState {
        object Enabled : SyncInternetState()
        sealed class Disabled : SyncInternetState() {
            object Offline : Disabled()
            object NoData : Disabled()
            object AlreadySynced : Disabled()
        }
    }
}
