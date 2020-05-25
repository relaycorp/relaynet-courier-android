package tech.relaycorp.courier.ui.main

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import tech.relaycorp.courier.background.InternetConnection
import tech.relaycorp.courier.background.InternetConnectionObserver
import tech.relaycorp.courier.background.WifiHotspotState
import tech.relaycorp.courier.background.WifiHotspotStateReceiver
import tech.relaycorp.courier.data.model.StorageSize
import tech.relaycorp.courier.data.model.StorageUsage
import tech.relaycorp.courier.domain.DeleteExpiredMessages
import tech.relaycorp.courier.domain.GetStorageUsage
import tech.relaycorp.courier.domain.ObserveCCACount
import tech.relaycorp.courier.test.factory.StorageUsageFactory
import tech.relaycorp.courier.test.factory.StoredMessageFactory
import tech.relaycorp.courier.test.test

internal class MainViewModelTest {

    private val connectionObserver = mock<InternetConnectionObserver>()
    private val hotspotStateReceiver = mock<WifiHotspotStateReceiver>()
    private val getStorageUsage = mock<GetStorageUsage>()
    private val observeCCACount = mock<ObserveCCACount>()
    private val deleteExpiredMessages = mock<DeleteExpiredMessages> {
        onBlocking { delete() }.thenReturn(emptyList())
    }

    @BeforeEach
    internal fun setUp() {
        whenever(connectionObserver.observe()).thenReturn(emptyFlow())
        whenever(hotspotStateReceiver.state()).thenReturn(emptyFlow())
        whenever(getStorageUsage.observe()).thenReturn(emptyFlow())
        whenever(observeCCACount.observe()).thenReturn(emptyFlow())
    }

    @Test
    internal fun syncPeopleState() {
        runBlocking {
            whenever(hotspotStateReceiver.state()).thenReturn(flowOf(WifiHotspotState.Disabled))
            whenever(connectionObserver.observe()).thenReturn(flow {
                delay(10)
                emit(InternetConnection.Offline)
                emit(InternetConnection.Online)
            })
            val viewModel = buildViewModel()
            val syncMode = viewModel.syncPeopleState().test(this)

            delay(100)
            syncMode
                .assertValues(
                    MainViewModel.SyncPeopleState.Enabled(WifiHotspotState.Disabled),
                    MainViewModel.SyncPeopleState.Disabled
                )
                .finish()
        }
    }

    @Test
    internal fun syncInternetState() {
        runBlocking {
            whenever(getStorageUsage.observe()).thenReturn(flowOf(StorageUsageFactory.build()))
            whenever(observeCCACount.observe()).thenReturn(flowOf(1L))
            whenever(connectionObserver.observe()).thenReturn(flow {
                delay(10)
                emit(InternetConnection.Offline)
                emit(InternetConnection.Online)
            })
            val viewModel = buildViewModel()
            val syncMode = viewModel.syncInternetState().test(this)

            delay(100)
            syncMode
                .assertValues(
                    MainViewModel.SyncInternetState.Disabled.Offline,
                    MainViewModel.SyncInternetState.Enabled
                )
                .finish()
        }
    }

    @Test
    internal fun storageUsage() = runBlocking {
        val storageUsage = StorageUsageFactory.build()
        whenever(getStorageUsage.observe()).thenReturn(flowOf(storageUsage))

        val viewModel = buildViewModel()
        assertEquals(storageUsage, viewModel.storageUsage().first())
    }

    @Test
    internal fun `deletes expired messages and show notice with size deleted`() = runBlocking {
        val messagesToDelete = listOf(StoredMessageFactory.build())
        whenever(deleteExpiredMessages.delete()).thenReturn(messagesToDelete)
        val viewModel = buildViewModel()
        assertEquals(
            messagesToDelete.first().size,
            viewModel.expiredMessagesDeleted().first()
        )
    }

    @Test
    internal fun `low storage message is visible`() = runBlocking {
        val storageUsage = StorageUsage(StorageSize(1), StorageSize(1))
        whenever(getStorageUsage.observe()).thenReturn(flowOf(storageUsage))

        val viewModel = buildViewModel()
        assertTrue(viewModel.lowStorageMessageIsVisible().first())
    }

    private fun buildViewModel() =
        MainViewModel(
            connectionObserver, hotspotStateReceiver, getStorageUsage, observeCCACount,
            deleteExpiredMessages
        )
}
