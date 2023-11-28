package tech.relaycorp.courier.ui.main

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateWatcher
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.data.model.StoredMessage
import tech.relaycorp.courier.domain.DeleteExpiredMessages
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.domain.ObserveCCACount
import tech.relaycorp.courier.ui.BaseViewModel
import javax.inject.Inject

class MainViewModel
    @Inject
    constructor(
        internetConnectionObserver: InternetConnectionObserver,
        hotspotStateReceiver: WifiHotspotStateWatcher,
        getStorageUsage: GetStorageUsage,
        observeCCACount: ObserveCCACount,
        deleteExpiredMessages: DeleteExpiredMessages,
    ) : BaseViewModel() {
        private val syncPeopleState = MutableStateFlow<SyncPeopleState?>(null)

        fun syncPeopleState() = syncPeopleState.asStateFlow().filterNotNull()

        private val syncInternetState = MutableStateFlow<SyncInternetState?>(null)

        fun syncInternetState() = syncInternetState.asStateFlow().filterNotNull()

        private val storageUsage = MutableStateFlow<StorageUsage?>(null)

        fun storageUsage() = storageUsage.asStateFlow().filterNotNull()

        private val lowStorageMessageIsVisible = MutableStateFlow(false)

        fun lowStorageMessageIsVisible() = lowStorageMessageIsVisible.asStateFlow()

        private val expiredMessagesDeleted = MutableStateFlow<StorageSize?>(null)

        fun expiredMessagesDeleted() = expiredMessagesDeleted.asStateFlow().filterNotNull()

        init {
            combine(
                internetConnectionObserver.observe(),
                hotspotStateReceiver.state(),
            ) { internetConnection, hotspotState ->
                when (internetConnection) {
                    InternetConnection.Offline -> SyncPeopleState.Enabled(hotspotState)
                    InternetConnection.Online -> SyncPeopleState.Disabled
                }
            }
                .onEach { syncPeopleState.value = it }
                .launchIn(scope)

            combine(
                internetConnectionObserver.observe(),
                getStorageUsage.observe(),
                observeCCACount.observe(),
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
                .onEach { syncInternetState.value = it }
                .launchIn(scope)

            getStorageUsage
                .observe()
                .onEach {
                    storageUsage.value = it
                    lowStorageMessageIsVisible.value = it.isLowOnSpace
                }
                .launchIn(scope)

            scope.launch {
                val messagesDeleted = deleteExpiredMessages.delete()
                if (messagesDeleted.any()) {
                    expiredMessagesDeleted.value = messagesDeleted.sumMessageSize()
                }
            }
        }

        private fun List<StoredMessage>.sumMessageSize() = map { it.size }.reduce { acc, messageSize -> acc + messageSize }

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
