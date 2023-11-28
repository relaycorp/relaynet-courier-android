package tech.relaycorp.courier.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateWatcher
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.domain.DeleteExpiredMessages
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.domain.ObserveCCACount
import tech.relaycorp.courier.test.WaitAssertions.waitForAssertEquals
import tech.relaycorp.courier.test.WaitAssertions.waitForAssertTrue
import tech.relaycorp.courier.test.factory.StorageUsageFactory
import tech.relaycorp.courier.test.factory.StoredMessageFactory

internal class MainViewModelTest {
    private val connectionObserver = mock<InternetConnectionObserver>()
    private val hotspotStateReceiver = mock<WifiHotspotStateWatcher>()
    private val getStorageUsage = mock<GetStorageUsage>()
    private val observeCCACount = mock<ObserveCCACount>()
    private val deleteExpiredMessages =
        mock<DeleteExpiredMessages> {
            onBlocking { delete() }.thenReturn(emptyList())
        }

    @BeforeEach
    internal fun setUp() {
        whenever(connectionObserver.observe()).thenReturn(emptyFlow())
        whenever(hotspotStateReceiver.state()).thenReturn(MutableStateFlow(WifiHotspotState.Disabled))
        whenever(getStorageUsage.observe()).thenReturn(emptyFlow())
        whenever(observeCCACount.observe()).thenReturn(emptyFlow())
    }

    @Test
    internal fun syncPeopleState() =
        runBlockingTest {
            val connectionStateFlow = MutableStateFlow(InternetConnection.Offline)
            whenever(hotspotStateReceiver.state()).thenReturn(MutableStateFlow(WifiHotspotState.Disabled))
            whenever(connectionObserver.observe()).thenReturn(connectionStateFlow)
            val viewModel = buildViewModel()

            viewModel.scope.launch {
                waitForAssertEquals(
                    MainViewModel.SyncPeopleState.Enabled(WifiHotspotState.Disabled),
                    viewModel.syncPeopleState()::first,
                )

                connectionStateFlow.value = InternetConnection.Online
                waitForAssertEquals(
                    MainViewModel.SyncPeopleState.Disabled,
                    viewModel.syncPeopleState()::first,
                )
            }
        }

    @Test
    internal fun syncInternetState() =
        runBlockingTest {
            val connectionStateFlow = MutableStateFlow(InternetConnection.Offline)
            whenever(getStorageUsage.observe()).thenReturn(flowOf(StorageUsageFactory.build()))
            whenever(observeCCACount.observe()).thenReturn(flowOf(1L))
            whenever(connectionObserver.observe()).thenReturn(connectionStateFlow)
            val viewModel = buildViewModel()
            viewModel.scope.launch {
                waitForAssertEquals(
                    MainViewModel.SyncInternetState.Disabled.Offline,
                    viewModel.syncInternetState()::first,
                )

                connectionStateFlow.value = InternetConnection.Online
                waitForAssertEquals(
                    MainViewModel.SyncInternetState.Enabled,
                    viewModel.syncInternetState()::first,
                )
            }
        }

    @Test
    internal fun storageUsage() =
        runBlockingTest {
            val storageUsage = StorageUsageFactory.build()
            whenever(getStorageUsage.observe()).thenReturn(flowOf(storageUsage))

            val viewModel = buildViewModel()
            viewModel.scope.launch {
                waitForAssertEquals(storageUsage, viewModel.storageUsage()::first)
            }
        }

    @Test
    internal fun `deletes expired messages and show notice with size deleted`() =
        runBlockingTest {
            val messagesToDelete = listOf(StoredMessageFactory.build())
            whenever(deleteExpiredMessages.delete()).thenReturn(messagesToDelete)
            val viewModel = buildViewModel()

            viewModel.scope.launch {
                waitForAssertEquals(
                    messagesToDelete.first().size,
                    viewModel.expiredMessagesDeleted()::first,
                )
            }
        }

    @Test
    internal fun `low storage message is visible`() =
        runBlockingTest {
            val storageUsage = StorageUsage(StorageSize(1), StorageSize(1))
            whenever(getStorageUsage.observe()).thenReturn(flowOf(storageUsage))

            val viewModel = buildViewModel()
            viewModel.scope.launch {
                waitForAssertTrue { viewModel.lowStorageMessageIsVisible().first() }
            }
        }

    private fun buildViewModel() =
        MainViewModel(
            connectionObserver,
            hotspotStateReceiver,
            getStorageUsage,
            observeCCACount,
            deleteExpiredMessages,
        )
}
